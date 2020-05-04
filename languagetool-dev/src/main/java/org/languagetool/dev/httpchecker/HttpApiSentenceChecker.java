/* LanguageTool, a natural language style checker
 * Copyright (C) 2020 Daniel Naber (http://www.danielnaber.de)
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301
 * USA
 */
package org.languagetool.dev.httpchecker;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.file.Files;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.*;

/**
 * Checks sentences by posting them to the HTTP API from several threads,
 * merging the result. Can be used for nightly regression tests.
 */
class HttpApiSentenceChecker {

  private final String baseUrl;
  private final String langCode;
  private final int threadCount;
  private final String token;

  public HttpApiSentenceChecker(CommandLine cmd) {
    baseUrl = cmd.hasOption("url") ? cmd.getOptionValue("url") : "https://api.languagetool.org";
    if (baseUrl.endsWith("/")) {
      throw new IllegalArgumentException("Don't let baseUrl end with a '/': " + baseUrl + ". Correct example: 'https://api.languagetool.org'");
    }
    langCode = cmd.getOptionValue("lang");
    threadCount = Integer.parseInt(cmd.getOptionValue("threads"));
    token = cmd.hasOption("token") ? cmd.getOptionValue("token") : null;
  }

  private void run(File input, File output) throws IOException, InterruptedException, ExecutionException {
    long t1 = System.currentTimeMillis();
    int lines = countLines(input);
    System.out.println(input + " has " + lines + " lines");
    List<File> files = new ArrayList<>();
    try {
      files = splitInput(lines, input, threadCount);
      List<File> resultFiles = runOnFiles(files);
      joinResults(resultFiles, output);
    } finally {
      for (File file : files) {
        FileUtils.deleteQuietly(file);
      }
    }
    long t2 = System.currentTimeMillis();
    Duration duration = Duration.of(t2 - t1, ChronoUnit.MILLIS);
    System.out.println("Runtime: " + formatDuration(duration) + " (h:mm:ss)");
  }

  private String formatDuration(Duration duration) {
    long seconds = duration.getSeconds();
    long absSeconds = Math.abs(seconds);
    return String.format("%d:%02d:%02d", absSeconds / 3600, (absSeconds % 3600) / 60, absSeconds % 60);
  }

  private int countLines(File input) throws FileNotFoundException {
    int count = 0;
    try (Scanner sc = new Scanner(input)) {
      while (sc.hasNextLine()) {
        sc.nextLine();
        count++;
      }
    }
    return count;
  }

  private List<File> splitInput(int lines, File input, int threadCount) throws IOException {
    List<File> tempFiles = new ArrayList<>();
    int batchSize = lines / threadCount;
    System.out.println("Working with " + threadCount + " threads, single batch size: " + batchSize + " lines");
    int fileCount = 0;
    int startLine = 0;
    int lineCount = 0;
    File tempFile = getTempFile(fileCount);
    tempFiles.add(tempFile);
    FileWriter fw = new FileWriter(tempFile);
    try (Scanner sc = new Scanner(input)) {
      while (sc.hasNextLine()) {
        String line = sc.nextLine();
        fw.write(line + "\n");
        lineCount++;
        if (lineCount > 0 && lineCount % batchSize == 0) {
          fileCount++;
          fw.close();
          File destFile = new File(tempFile.getParentFile(), HttpApiSentenceChecker.class.getSimpleName() + "-" + startLine + "-to-" + lineCount + ".json");
          startLine = lineCount;
          FileUtils.moveFile(tempFile, destFile);
          tempFile = getTempFile(fileCount);
          tempFiles.add(destFile);
          fw = new FileWriter(tempFile);
        }
      }
    }
    fw.close();
    return tempFiles;
  }

  @NotNull
  private File getTempFile(int fileCount) throws IOException {
    return File.createTempFile(HttpApiSentenceChecker.class.getSimpleName() + "-split-input-" + fileCount + "-", ".txt");
  }

  private List<File> runOnFiles(List<File> files) throws InterruptedException, ExecutionException {
    List<File> resultFiles = new ArrayList<>();
    ExecutorService execService = new ForkJoinPool(threadCount, ForkJoinPool.defaultForkJoinWorkerThreadFactory, null, false);
    List<Callable<List<File>>> callables = new ArrayList<>();
    int count = 0;
    for (File file : files) {
      if (file.length() == 0) {
        continue;
      }
      callables.add(new CheckCallable(count, baseUrl, token, file, langCode));
      count++;
    }
    List<Future<List<File>>> futures = execService.invokeAll(callables);
    for (Future<List<File>> future : futures) {
      resultFiles.addAll(future.get());
    }
    execService.shutdownNow();
    return resultFiles;
  }

  private void joinResults(List<File> resultFiles, File output) throws IOException {
    System.out.println("Joining " + resultFiles.size() + " result files...");
    ObjectMapper mapper = new ObjectMapper(new JsonFactory());
    ArrayNode arrayNode = mapper.createArrayNode();
    resultFiles.sort(Comparator.naturalOrder());
    Set<String> buildDates = new HashSet<>();
    for (File resultFile : resultFiles) {
      // each thread has just appended the JSON data, one JSON result per line,
      // we now put it into a real JSON, i.e. adding the results into a JSON array:
      List<String> lines = Files.readAllLines(resultFile.toPath());
      //System.out.println("Loading and merging " + lines.size() + " lines from " + resultFile);
      for (String line : lines) {
        JsonNode node = mapper.readTree(line);
        buildDates.add(node.get("software").get("buildDate").asText());
        arrayNode.add(node);
      }
      FileUtils.deleteQuietly(resultFile);
    }
    if (buildDates.size() > 1) {
      System.err.println("-----------------------------------------------------");
      System.err.println("WARNING: inconsistent build dates across API servers: Found " + buildDates);
      System.err.println("-----------------------------------------------------");
    } else {
      System.out.println("All requests answered by API servers with build date " + buildDates);
    }
    try (FileWriter fw = new FileWriter(output)) {
      fw.write(arrayNode.toString());
    }
    System.out.println("Joined result stored at " + output);
  }

  public static void main(String[] args) throws Exception {
    Options options = new Options();
    options.addRequiredOption(null, "input", true, "Plain text input file");
    options.addRequiredOption(null, "lang", true, "Language code, e.g. en-US");
    options.addRequiredOption(null, "threads", true, "Number of threads");
    options.addRequiredOption(null, "output", true, "Output file");
    options.addOption(null, "token", true, "Secret token to skip server's limits");
    options.addOption(null, "url", true, "Base URL, defaults to https://api.languagetool.org");
    CommandLine cmd = new DefaultParser().parse(options, args);
    HttpApiSentenceChecker checker = new HttpApiSentenceChecker(cmd);
    checker.run(new File(cmd.getOptionValue("input")), new File(cmd.getOptionValue("output")));
  }

}
