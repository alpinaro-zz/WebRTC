package antmedia.webrtctest;

import io.restassured.builder.RequestSpecBuilder;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import static io.restassured.RestAssured.given;

/**
 * Unit test for simple App.
 */
public class AppTest 
    extends TestCase
{
    public void testName() {

        // TODO Check auth

        RequestSpecification specAnt = new RequestSpecBuilder().setBaseUri("https://ovh36.antmedia.io:5443/v2").build();
        Response response;
        /*
        String broadcastId = "myStream";

        specAnt.pathParams("pp1", "broadcasts", "pp2", broadcastId);

        response = given().spec(specAnt).when().get("/{pp1}/{pp2}");

        response.prettyPrint();
         */
        // TODO Solve 404 response
        // TODO Get number of viewers from response

        specAnt.pathParam("pp1", "cpu-status");

        response = given().spec(specAnt).when().get("/{pp1}");

        response.prettyPrint();

        // TODO Solve 404 response
        // TODO Get cpu usage from response

        // TODO In a while loop, check cpu load from api and when 70% stop & log the number of viewers
    }

    /**
     * Create the test case
     *
     * @param testName name of the test case
     */

    public AppTest( String testName )
    {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( AppTest.class );
    }

    /**
     * Rigourous Test :-)
     */
    public void testApp()
    {
        assertTrue( true );
    }
}
