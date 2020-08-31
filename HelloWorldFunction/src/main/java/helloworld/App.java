package helloworld;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import java.util.HashMap;
import java.util.Map;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.internal.StaticCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.PutItemOutcome;
import com.amazonaws.services.dynamodbv2.document.spec.PutItemSpec;
import com.amazonaws.services.dynamodbv2.model.ConditionalCheckFailedException;
import com.amazonaws.services.lambda.runtime.*;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ListObjectsV2Result;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import helloworld.model.PersonRecord;

/**
 * Handler for requests to Lambda function.
 */
public class App implements RequestHandler<PersonRecord, APIGatewayProxyResponseEvent> {

    private DynamoDB dynamoDb;
    private String DYNAMODB_TABLE_NAME = "Person";
    private Regions REGION = Regions.AP_SOUTH_1;

    public APIGatewayProxyResponseEvent handleRequest(final PersonRecord input, final Context context) {

        LambdaLogger logger = context.getLogger();

        logger.log("Person details provided   " + input.getFirstName() + " " + input.getLastName());

        final AmazonS3 s3 = AmazonS3ClientBuilder.standard().withRegion(Regions.AP_SOUTH_1)
//                .withCredentials(new StaticCredentialsProvider(new BasicAWSCredentials("<ACCESS KEY>", "<SECRET KEY>")))
                .build();

        ListObjectsV2Result result =  s3.listObjectsV2("hbs-csv");
        result.getObjectSummaries().forEach(object -> System.out.println("object.getKey() = " + object.getKey()));

        String body = s3.getObjectAsString("hbs-csv", "cricketer.csv");
        logger.log("*BODY** " + body);

        this.initDynamoDbClient();
        persistData(input);


        return new APIGatewayProxyResponseEvent().withHeaders(headers())
                .withStatusCode(200)
                .withBody("Person is created!!");

    }

    private Map<String, String>  headers() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("X-Custom-Header", "application/json");
        return headers;
    }

    private PutItemOutcome persistData(PersonRecord personRecordRequest)
            throws ConditionalCheckFailedException {
        return this.dynamoDb.getTable(DYNAMODB_TABLE_NAME)

                .putItem(
                        new PutItemSpec()

                                .withItem(new Item()
                                        .withLong("id", personRecordRequest.getId())
                                        .withString("firstName", personRecordRequest.getFirstName())
                                        .withString("lastName", personRecordRequest.getLastName())));
    }

    private void initDynamoDbClient() {
        AmazonDynamoDBClient client = new AmazonDynamoDBClient(
                /*new AWSCredentials() {
                    @Override
                    public String getAWSAccessKeyId() {
                        return "<ACCESS KEY>";
                    }

                    @Override
                    public String getAWSSecretKey() {
                        return "<SECRET KEY>";
                    }
                }*/
        );

        client.setRegion(Region.getRegion(REGION));
        this.dynamoDb = new DynamoDB(client);
    }


}
