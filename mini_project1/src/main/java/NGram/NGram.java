package NGram;

import java.io.IOException;
import java.util.StringTokenizer;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

public class NGram {
    public static final String NGRAM = "ngram.n";

    public static class TokenizerMapper
            extends Mapper<Object, Text, Text, IntWritable>{


        private final static IntWritable one = new IntWritable(1);
        private Text word = new Text();
        private int n = 1;

        public void configure(Configuration jobConf) {
            n = jobConf.getInt(NGRAM, 1);
        }

        public void map(Object key, Text value, Context context
        ) throws IOException, InterruptedException {
            configure(context.getConfiguration());

            StringTokenizer itr = new StringTokenizer(value.toString());
            while (itr.hasMoreTokens()) {
                String word = itr.nextToken();
                if (word.length() >= n) {
                    for (int i = 0; i < word.length() - n + 1; ++i) {
                        String s = word.substring(i, i + n);
                        context.write(new Text(s), one);
                    }
                }
            }
        }
    }

    public static class IntSumReducer
            extends Reducer<Text,IntWritable,Text,IntWritable> {
        private IntWritable result = new IntWritable();

        public void reduce(Text key, Iterable<IntWritable> values,
                           Context context
        ) throws IOException, InterruptedException {
            int sum = 0;
            for (IntWritable val : values) {
                sum += val.get();
            }
            result.set(sum);
            context.write(key, result);
        }
    }

    public static void main(String[] args) throws Exception {
        Configuration conf = new Configuration();

        conf.setInt(NGRAM, Integer.parseInt(args[2]));
        Job job = Job.getInstance(conf, "N Gram");
        job.setJarByClass(NGram.class);
        job.setMapperClass(TokenizerMapper.class);
        job.setCombinerClass(IntSumReducer.class);
        job.setReducerClass(IntSumReducer.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(IntWritable.class);
        FileInputFormat.addInputPath(job, new Path(args[0]));
        FileOutputFormat.setOutputPath(job, new Path(args[1]));

        System.exit(job.waitForCompletion(true) ? 0 : 1);
    }
}

