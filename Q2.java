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

/* 2nd Question */
public class Q2 {

    /* mapper class */
    public static class TokenizerMapper
            extends Mapper<Object, Text, Text, Text> {

        private Text word = new Text();
        private Text fileTag = new Text();

        public void map(Object key, Text value, Context context)
                        throws IOException, InterruptedException {
            /* determines which file the data is coming from */
            String file = context.getInputSplit().toString();
            if (file.contains("pg46.txt")) {
                fileTag.set("pg46");
            } else if (file.contains("pg100.txt")) {
                fileTag.set("pg100");
            }

            /* tokenizes and writes each word with its file tag */
            StringTokenizer itr = new StringTokenizer(value.toString());
            while (itr.hasMoreTokens()) {
                /* ensures comparisons are case-insensitive, e.g. considers hello equal to HELLO */
                word.set(itr.nextToken().toLowerCase());
                if (!word.toString().isEmpty()) {
                    context.write(word, fileTag);
                }
            }
        }
    }

    /* reducer class */
    public static class FilterReducer extends Reducer<Text, Text, Text, Text> {

        public void reduce(Text key, Iterable<Text> values, Context context)
                                    throws IOException, InterruptedException {

            Set<String> tags = new HashSet<>();
            for (Text val : values) {
                    tags.add(val.toString());
            }

            /* outputs the word if it appears in pg46.txt, but not in pg100.txt */
            if (tags.contains("pg46") && !tags.contains("pg100")) {
                context.write(key, new Text("")); //empty text instead of a word counter
            }
        }
    }

    public static void main(String[] args) throws Exception {
        
        Configuration conf = new Configuration();
        /* for our needs, /user/root/input/ contains 2 files */
        Job job = Job.getInstance(conf, "Q2: Display of words that appear in pg46.txt, but not in pg100.txt");
        job.setJarByClass(Q2.class);
        job.setMapperClass(TokenizerMapper.class);
        job.setReducerClass(FilterReducer.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(Text.class);
        FileInputFormat.addInputPath(job, new Path(args[0])); // /user/root/input/
        FileOutputFormat.setOutputPath(job, new Path(args[1])); // /user/root/output/
        System.exit(job.waitForCompletion(true) ? 0 : 1);
    }
}
