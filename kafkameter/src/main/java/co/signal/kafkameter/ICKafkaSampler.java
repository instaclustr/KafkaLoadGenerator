package co.signal.kafkameter;

import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.protocol.java.sampler.AbstractJavaSamplerClient;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Properties;

public class ICKafkaSampler extends AbstractJavaSamplerClient {

    private static final String PARAMETER_PRODUCER_PROP_FILE = "Connection parameters:";
    private static final String PARAMETER_KAFKA_TOPIC = "kafka_topic";
    private static final String PARAMETER_KEY_SERIALIZER = "key.serializer";
    private static final String PARAMETER_VALUE_SERIALIZER= "value.serializer";

    private KafkaProducer<Long, Long> producer;
    private long counter=0;

    @Override
    public void setupTest(JavaSamplerContext context) {
        Properties props = new Properties();
        try {
            props.load(new FileReader(context.getParameter(PARAMETER_PRODUCER_PROP_FILE)));
        }catch (IOException e){
            e.printStackTrace();
        }

        props.put("key.serializer", context.getParameter(PARAMETER_KEY_SERIALIZER));
        props.put("value.serializer", context.getParameter(PARAMETER_VALUE_SERIALIZER));
//        System.out.print("\n\nKafka Properties("+Thread.currentThread().getName()+"):\n"+props+"\n\n");
        System.out.println("Starting: "+Thread.currentThread().getName());
        producer = new KafkaProducer<Long, Long>(props);
    }
    @Override
    public SampleResult runTest(JavaSamplerContext javaSamplerContext) {
        SampleResult result = newSampleResult();
        final ProducerRecord<Long, Long> producerRecord;
        counter=counter+1;
        sampleResultStart(result, ""+counter);

        producerRecord = new ProducerRecord<Long, Long>(javaSamplerContext.getParameter(PARAMETER_KAFKA_TOPIC),new Long(counter),new Long(counter));

    try {
        producer.send(producerRecord);
        producer.flush();
        sampleResultSuccess(result, null);
        } catch (Exception e) {
        System.out.print(e.getStackTrace());
        sampleResultFailed(result, "500", e);
        }
        return result;
    }

    @Override
    public void teardownTest(JavaSamplerContext context) {
        producer.close();
    }

    @Override
    public Arguments getDefaultParameters() {
        Arguments defaultParameters = new Arguments();
        defaultParameters.addArgument(PARAMETER_PRODUCER_PROP_FILE, "${PRODUCER_PROP_FILE}");
        defaultParameters.addArgument(PARAMETER_KAFKA_TOPIC, "${PARAMETER_KAFKA_TOPIC}");
        defaultParameters.addArgument(PARAMETER_KEY_SERIALIZER, "${PARAMETER_KEY_SERIALIZER}");
        defaultParameters.addArgument(PARAMETER_VALUE_SERIALIZER, "${PARAMETER_VALUE_SERIALIZER}");

        return defaultParameters;
    }

    /**
     * Use UTF-8 for encoding of strings
     */
    private static final String ENCODING = "UTF-8";

    /**
     * Factory for creating new {@link SampleResult}s.
     */
    private SampleResult newSampleResult() {
        SampleResult result = new SampleResult();
        result.setDataEncoding(ENCODING);
        result.setDataType(SampleResult.TEXT);
        return result;
    }

    /**
     * Start the sample request and set the {@code samplerData} to {@code data}.
     *
     * @param result
     *          the sample result to update
     * @param data
     *          the request to set as {@code samplerData}
     */
    private void sampleResultStart(SampleResult result, String data) {
        result.setSamplerData(data);
        result.sampleStart();
    }

    /**
     * Mark the sample result as {@code end}ed and {@code successful} with an "OK" {@code responseCode},
     * and if the response is not {@code null} then set the {@code responseData} to {@code response},
     * otherwise it is marked as not requiring a response.
     *
     * @param result sample result to change
     * @param response the successful result message, may be null.
     */
    private void sampleResultSuccess(SampleResult result, /* @Nullable */ String response) {
        result.sampleEnd();
        result.setSuccessful(true);
        result.setResponseCodeOK();
        if (response != null) {
            result.setResponseData(response, ENCODING);
        }
        else {
            result.setResponseData("No response required", ENCODING);
        }
    }

    /**
     * Mark the sample result as @{code end}ed and not {@code successful}, and set the
     * {@code responseCode} to {@code reason}.
     *
     * @param result the sample result to change
     * @param reason the failure reason
     */
    private void sampleResultFailed(SampleResult result, String reason) {
        result.sampleEnd();
        result.setSuccessful(false);
        result.setResponseCode(reason);
    }

    /**
     * Mark the sample result as @{code end}ed and not {@code successful}, set the
     * {@code responseCode} to {@code reason}, and set {@code responseData} to the stack trace.
     *
     * @param result the sample result to change
     * @param exception the failure exception
     */
    private void sampleResultFailed(SampleResult result, String reason, Exception exception) {
        sampleResultFailed(result, reason);
        result.setResponseMessage("Exception: " + exception);
        result.setResponseData(getStackTrace(exception), ENCODING);
    }

    /**
     * Return the stack trace as a string.
     *
     * @param exception the exception containing the stack trace
     * @return the stack trace
     */
    private String getStackTrace(Exception exception) {
        StringWriter stringWriter = new StringWriter();
        exception.printStackTrace(new PrintWriter(stringWriter));
        return stringWriter.toString();
    }

    }
