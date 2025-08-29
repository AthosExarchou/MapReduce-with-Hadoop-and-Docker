# MapReduce with Hadoop and Docker

Developed as for the **Database Design** course at [Harokopio University of Athens – Dept. of Informatics and Telematics](https://www.dit.hua.gr), this project demonstrates how to run **Hadoop MapReduce jobs** inside a Dockerized Hadoop cluster.
The assignments (Q1–Q4) are implemented in Java and packaged as a `.jar` to run inside the Hadoop Namenode container.

## Project Overview
The goal is to practice Hadoop’s distributed processing with text data from **Project Gutenberg** and other sources.  
The MapReduce jobs answer the following questions:

- **Q1**: Count the frequency of each word across multiple input files.
- **Q2**: Display words that appear in `pg46.txt` but **not** in `pg100.txt`.
- **Q3**: Build an *inverted index* (show which file(s) each word belongs to).
- **Q4**: Counts words (≥4 characters) that appear in at least two of the three input files, exporting results in CSV format with per-file counts.

---

### Logic & Pseudocode for Each Job

#### Q1 – Word Count
- **Mapper:** Emit `(word, 1)` for each word in the input.
- **Reducer:** Sum counts per word to get total occurrences.
- **Pseudocode:**
```
map(line):
  for word in line.split():
    emit(word, 1)

reduce(word, counts):
  total = sum(counts)
  emit(word, total)
```

#### Q2 – Word Difference (File1 – File2)
- **Mapper:** Tag each word with its source file.
- **Reducer:** Collect files for each word.  
  Keep only words that appear in File1 but not in File2.
- **Pseudocode:**
```
map(line, fileName):
  for word in line.split():
    emit(word, fileName)

reduce(word, files):
  if word appears in file1 only:
    emit(word, "")
```

#### Q3 – Inverted Index
- **Mapper:** Emit `(word, fileName)` for each word.
- **Reducer:** Gather list of distinct files for each word.
  - **Pseudocode:**
```
map(line, fileName):
  for word in line.split():
    emit(word, fileName)

reduce(word, fileNames):
  uniqueFiles = set(fileNames)
  emit(word, uniqueFiles)
```

#### Q4 – Multi-file Word Count (≥4 chars, at least 2 files)
- **Mapper:** Emit `(word, fileName)` for words with length ≥ 4.
- **Reducer:** Count occurrences per file.  
  Keep only words appearing in ≥2 files.  
  Output in CSV format: `word, count_in_pg100, count_in_pg46, count_in_el_quijote`.
- **Pseudocode:**
```
Map Phase:
  For each line in the input:
    Extract fileName from FileSplit
    For each word in the line:
      Lowercase the word
      If word length >= 4:
        Emit (word, fileName)

Reduce Phase:
  For each unique word:
    Count occurrences of the word per file (pg100.txt, pg46.txt, el_quijote.txt)
    If the word appears in at least 2 different files:
      Format output as CSV: "word, pg100Count, pg46Count, el_quijoteCount"
      Emit (CSV row, "")
```

---

## Prerequisites
Before you start, make sure you have:

- [Java JDK 21+](https://www.oracle.com/java/technologies/downloads/#jdk21-windows)
- [Maven](https://maven.apache.org/) (for building the project)
- [Docker](https://docs.docker.com/get-docker/) and [Docker Compose](https://docs.docker.com/compose/)
- Internet access to download input text files

---

## Getting Started

### 1. Start the Hadoop Docker cluster
```bash
docker-compose up -d
```

### 2. Access the Namenode and Initialize HDFS
```bash
docker exec -it namenode bash

hdfs dfs -ls /
hdfs dfs -mkdir -p /user/root
hdfs dfs -ls /user/
```

### 3. Download Input Files
Exit the container, then run:
```bash
wget https://gist.githubusercontent.com/jsdario/6d6c69398cb0c73111e49f1218960f79/raw/8d4fc4548d437e2a7203a5aeeace5477f598827d/el_quijote.txt
wget https://www.gutenberg.org/cache/epub/100/pg100.txt
wget https://www.gutenberg.org/cache/epub/46/pg46.txt
```

### 4. Copy JAR + Files into Namenode
```bash
docker cp ../WordCount/target/MapReduceProject-1.0-SNAPSHOT.jar namenode:/tmp
docker cp el_quijote.txt namenode:/tmp
docker cp pg46.txt namenode:/tmp
docker cp pg100.txt namenode:/tmp
```

### 5. Prepare HDFS Input Directory
```bash
docker exec -it namenode bash

hdfs dfs -mkdir /user/root/input
cd /tmp
hdfs dfs -put el_quijote.txt /user/root/input
hdfs dfs -put pg46.txt /user/root/input
hdfs dfs -put pg100.txt /user/root/input
```

---

## Build & Run Instructions

### Building the JAR
```bash
mvn clean install
mvn package
```

This will produce a JAR file under:
```text
target/MapReduceProject-1.0-SNAPSHOT.jar
```

### General Job Execution
```bash
hadoop jar MapReduceProject-1.0-SNAPSHOT.jar input output
```

### Q1: Word Count Across Multiple Files

**Description:** Counts how many times each word appears across all input files (`el_quijote.txt`, `pg46.txt`, `pg100.txt`).
Class: `org.example.Q1`

Run:
```bash
hadoop jar MapReduceProject-1.0-SNAPSHOT.jar org.example.Q1 /user/root/input /user/root/output
```

View Results:
```bash
hdfs dfs -cat /user/root/output/part-r-00000
```

### Q2: Words in `pg46.txt` but not in `pg100.txt`

**Description:** Identifies unique words that exist in `pg46.txt` but are missing from `pg100.txt`.
Class: `org.example.Q2`

Run:
```bash
hadoop jar MapReduceProject-1.0-SNAPSHOT.jar org.example.Q2 /user/root/input /user/root/output
```

View Results:
```bash
hdfs dfs -cat /user/root/output/part-r-00000
```

### Q3: Inverted Index

**Description:** Produces an index mapping each word to the list of files it appears in.
Class: `org.example.Q3`

Run:
```bash
hadoop jar MapReduceProject-1.0-SNAPSHOT.jar org.example.Q3 /user/root/input /user/root/output
```

View Results:
```bash
hdfs dfs -cat /user/root/output/part-r-00000
```

### Q4: Common Words Across Multiple Files (CSV Export)
**Description**:  
Finds words of at least 4 characters that appear in **at least two of the three files** (`pg100.txt`, `pg46.txt`, `el_quijote.txt`).  
For each qualifying word, the program outputs the count of occurrences in each file in **CSV format**.

**Class**: `org.example.Q4`

**Run:**
```bash
hadoop jar MapReduceProject-1.0-SNAPSHOT.jar org.example.Q4 /user/root/input /user/root/output
```

View Results (CSV output):
```bash
hdfs dfs -cat /user/root/output/Q4_wc.csv/part-r-00000
```

Example Output (CSV):
```text
amor, 45, 31, 78
king, 12, 7, 0
```
*(Format: `word, pg100_count, pg46_count, el_quijote_count`)*

#### Exporting Q4 Results

Inside the Namenode:
```bash
hdfs dfs -cat /user/root/output/Q4_wc.csv/part-r-00000 > /tmp/Q4_results.csv
```

Then copy to your local machine:
```bash
docker cp namenode:/tmp/Q4_results.csv ../WordCount/
```

---

## Exporting Output to Local Files

Inside the Namenode:
```bash
hdfs dfs -cat /user/root/output/part-r-00000 > /tmp/QX_results.txt
```

Then copy to your local machine:
```bash
docker cp namenode:/tmp/QX_results.txt ../WordCount/
```
*(Replace QX with Q1, Q2, or Q3.)*

## Cleanup

Remove HDFS output/input before re-running jobs:
```bash
hadoop fs -rm -r /user/root/output
hadoop fs -rm -r /user/root/input
```

Shut down the cluster:
```bash
exit
docker-compose down
```

## Notes

- Ensure the output directory **does not exist** before running a job (Hadoop requirement).
- For large datasets, Hadoop will distribute processing across containers automatically.
- Results can be further processed or visualized using external tools.

---

## Author

- **Name**: Exarchou Athos
- **Student ID**: it2022134
- **Email**: it2022134@hua.gr, athosexarhou@gmail.com

## License
This project is licensed under the MIT License.
