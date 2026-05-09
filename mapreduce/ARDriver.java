package assigment2;

import java.io.IOException;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

public class ARDriver {

    public static class MovieMapper extends Mapper<LongWritable, Text, Text, Text> {
        public void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {

            String line = value.toString();

            if (key.get() == 0 || line.startsWith("name,released_at")) {
                return;
            }

            String[] columns = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");

            try {
                String releasedAt = columns[1].replace("\"", "").trim();
                String genreFull = columns[2].replace("\"", "").trim();
                String type = columns[5].replace("\"", "").trim();
                String imdbRating = columns[7].replace("\"", "").trim();

                if (releasedAt.isEmpty() || genreFull.isEmpty() || type.isEmpty() || imdbRating.isEmpty()) {
                    return;
                }

                String firstGenre = genreFull.split(",")[0].trim();

                String yearStr = releasedAt.split("-")[0];
                int year = Integer.parseInt(yearStr);
                String decade = (year / 10 * 10) + "s";

                double rating = Double.parseDouble(imdbRating.split("/")[0]);

                String outputKey = type + ", " + firstGenre + ", " + decade;

                context.write(
                    new Text(outputKey),
                    new Text("1," + rating + "," + rating + "," + rating)
                );

            } catch (Exception e) {
                return;
            }
        }
    }

    public static class MovieCombiner extends Reducer<Text, Text, Text, Text> {
        public void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {

            int count = 0;
            double sum = 0;
            double min = Double.MAX_VALUE;
            double max = Double.MIN_VALUE;

            for (Text val : values) {
                String[] parts = val.toString().split(",");

                int c = Integer.parseInt(parts[0]);
                double s = Double.parseDouble(parts[1]);
                double mn = Double.parseDouble(parts[2]);
                double mx = Double.parseDouble(parts[3]);

                count += c;
                sum += s;
                min = Math.min(min, mn);
                max = Math.max(max, mx);
            }

            context.write(new Text(key), new Text(count + "," + sum + "," + min + "," + max));
        }
    }

    public static class MovieReducer extends Reducer<Text, Text, Text, NullWritable> {
    	
    	private boolean headerWritten = false;
    	
        public void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
        	
        	 if (!headerWritten) {
                 context.write(
                     new Text("Type, Genre, Decade, Count, AverageRating, MinRating, MaxRating"),
                     NullWritable.get()
                 );
                 headerWritten = true;
             }

            int count = 0;
            double sum = 0;
            double min = Double.MAX_VALUE;
            double max = Double.MIN_VALUE;

            for (Text val : values) {
                String[] parts = val.toString().split(",");

                int c = Integer.parseInt(parts[0]);
                double s = Double.parseDouble(parts[1]);
                double mn = Double.parseDouble(parts[2]);
                double mx = Double.parseDouble(parts[3]);

                count += c;
                sum += s;
                min = Math.min(min, mn);
                max = Math.max(max, mx);
            }

            double average = sum / count;

            String result = key.toString() + ", " + count + ", " +
                    String.format("%.2f", average) + ", " +
                    String.format("%.1f", min) + ", " +
                    String.format("%.1f", max);

            context.write(new Text(result), NullWritable.get());
        }
    }

    public static void main(String[] args) throws Exception {

        Configuration conf = new Configuration();

        Job job = Job.getInstance(conf, "Movie Rating Statistics");

        job.setJarByClass(ARDriver.class);

        job.setMapperClass(MovieMapper.class);
        job.setCombinerClass(MovieCombiner.class);
        job.setReducerClass(MovieReducer.class);

        job.setMapOutputKeyClass(Text.class);
        job.setMapOutputValueClass(Text.class);

        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(NullWritable.class);

        FileInputFormat.addInputPath(job, new Path(args[1]));
        FileOutputFormat.setOutputPath(job, new Path(args[2]));

        System.exit(job.waitForCompletion(true) ? 0 : 1);
    }
}
