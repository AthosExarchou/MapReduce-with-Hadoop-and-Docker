package org.example;

/* imports */
import java.io.IOException;
import java.util.*;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

/* 4th Question */
public class Q4 {

    public static class TokenizerMapper
            extends Mapper<Object, Text, Text, Text> {

        private Text word = new Text();
        private Text fileTag = new Text();

        public void map(Object key, Text value, Context context)
                throws IOException, InterruptedException {
            /* gets the file name with FileSplit */
            String fileName = ((org.apache.hadoop.mapreduce.lib.input.FileSplit)
                                    context.getInputSplit()).getPath().getName(); //cast
            fileTag.set(fileName);

            StringTokenizer itr = new StringTokenizer(value.toString());
            while (itr.hasMoreTokens()) {
                /* ensures comparisons are case-insensitive, e.g. considers hello equal to HELLO */
                word.set(itr.nextToken().toLowerCase());

                /* processes only the words with 4 or more characters */
                if (word.toString().length() >= 4) {
                    context.write(word, fileTag);
                }
            }
        }
    }

    public static class WordCountReducer extends Reducer<Text, Text, Text, Text> {

        public void reduce(Text key, Iterable<Text> values, Context context)
                throws IOException, InterruptedException {

            /* counts number of occurrences */
            Map<String, Integer> fileCounts = new HashMap<>();
            for (Text val : values) {
                String fileName = val.toString();
                fileCounts.put(fileName, fileCounts.getOrDefault(fileName, 0) + 1);
            }

            /* considers only the words that appear in two files at least */
            if (fileCounts.size() >= 2) {
                /* counts occurrences for each file (avoids null values with the getOrDefault() method)*/
                int pg100Count = fileCounts.getOrDefault("pg100.txt", 0);
                int pg46Count = fileCounts.getOrDefault("pg46.txt", 0);
                int el_quijoteCount = fileCounts.getOrDefault("el_quijote.txt", 0);

                /* writes the word and its counts for each file in CSV format */
                String csvOutput = key.toString() + ", " + pg100Count + ", " + pg46Count + ", " + el_quijoteCount;

                /* writes the CSV row with an empty value */
                context.write(new Text(csvOutput), new Text(""));
            }
        }
    }

    public static void main(String[] args) throws Exception {
    
        Configuration conf = new Configuration();
        /* for our needs, /user/root/input/ contains 3 files */
        Job job = Job.getInstance(conf, "Q4: Word counts across at least 2 files,"+
                                                "where the word consists of at least 4 characters");
        job.setJarByClass(Q4.class);
        job.setMapperClass(TokenizerMapper.class);
        job.setReducerClass(WordCountReducer.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(Text.class);

        /* input directory */
        FileInputFormat.addInputPath(job, new Path(args[0])); /* /user/root/input/ */
        /* output directory and file */
        String outputDir = args[1]; /* /user/root/output/ */
        String outputFile = "Q4_wc.csv"; // output filename
        /* /user/root/output/Q4_wc.csv */
        FileOutputFormat.setOutputPath(job, new Path(outputDir + "/" + outputFile));

        System.exit(job.waitForCompletion(true) ? 0 : 1);
    }
}
