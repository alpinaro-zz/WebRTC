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
