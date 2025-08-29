package org.example;

/* imports */
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

/* 3rd Question */
public class Q3 {

    /* mapper class */
    public static class TokenizerMapper
            extends Mapper<Object, Text, Text, Text> {

        private Text word = new Text();
        private Text fileTag = new Text();

        public void map(Object key, Text value, Context context)
                throws IOException, InterruptedException {
            /* extracts the file name using FileSplit */
            String fileName = ((org.apache.hadoop.mapreduce.lib.input.FileSplit)
                                    context.getInputSplit()).getPath().getName();

            /* sets the file name as the tag */
            fileTag.set(fileName);

            /* tokenize the input line and write (word, fileName) */
            StringTokenizer itr = new StringTokenizer(value.toString());
            while (itr.hasMoreTokens()) {
                /* ensures comparisons aren't case-sensitive, e.g. considers hello equal to HELLO */
                word.set(itr.nextToken().toLowerCase());
                if (!word.toString().isEmpty()) {
                    context.write(word, fileTag);
                }
            }
        }
    }

    /* reducer class */
    public static class InvertedIndexReducer extends Reducer<Text, Text, Text, Text> {

        public void reduce(Text key, Iterable<Text> values, Context context)
                throws IOException, InterruptedException {

            Set<String> fileNames = new HashSet<>();
            for (Text val : values) {
                fileNames.add(val.toString());
            }

            /* combines file names into a single string */
            String result = String.join(", ", fileNames);

            /* writes the word and the list of file names */
            context.write(key, new Text(result));
        }
    }

    public static void main(String[] args) throws Exception {
        
        Configuration conf = new Configuration();
        /* for our needs, /user/root/input/ contains 3 files */
        Job job = Job.getInstance(conf, "Q3: Display which file(s) a word belongs to");
        job.setJarByClass(Q3.class);
        job.setMapperClass(TokenizerMapper.class);
        job.setReducerClass(InvertedIndexReducer.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(Text.class);
        FileInputFormat.addInputPath(job, new Path(args[0])); // /user/root/input/
        FileOutputFormat.setOutputPath(job, new Path(args[1])); // /user/root/output/
        System.exit(job.waitForCompletion(true) ? 0 : 1);
    }
}
