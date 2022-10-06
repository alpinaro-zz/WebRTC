package antmedia.webrtctest;

import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.json.JSONObject;

import static io.restassured.RestAssured.given;

/**
 * Unit test for simple App.
 */
public class AppTest 
    extends TestCase
{
    public void testName() {

        // TODO Check auth

        RequestSpecification specAnt = new RequestSpecBuilder().setBaseUri("https://ovh36.antmedia.io:5443/rest/v2").build();
        Response response;

        JSONObject body = new JSONObject();
        body.put("email", "alper.cinaroglu@gmail.com");
        body.put("userType", "ADMIN");
        body.put("scope", "system");
        body.put("fullName", "Alper Çınaroğlu");
        body.put("firstName", "Alper");
        body.put("lastName", "Çınaroğlu");
        body.put("picture", "");

        //specAnt.pathParam("pp1", "addUser"); // addInitialUser
        //specAnt.pathParams("pp1", "users", "pp2", "initial");
        //response = given().spec(specAnt).contentType("application/json").accept("application/json").when().body(body.toString()).post("/{pp1}");
        //response = given().spec(specAnt).contentType("application/json").accept("application/json").when().body(body.toString()).post("/{pp1}/{pp2}");

        String broadcastId = "string";

        specAnt.pathParams("pp1", "broadcasts", "pp2", broadcastId);

        response = given().spec(specAnt).when().get("/{pp1}/{pp2}");

        response.prettyPrint();

        // TODO Solve 404 response
        // TODO Get number of viewers from response

        //specAnt.pathParam("pp1", "authentication-status"); // cpu-status authentication-status user-list

        //response = given().spec(specAnt).when().get("/{pp1}");

        //response.prettyPrint();

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
