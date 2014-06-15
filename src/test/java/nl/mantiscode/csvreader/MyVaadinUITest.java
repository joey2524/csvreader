/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package nl.mantiscode.csvreader;

import com.vaadin.server.VaadinRequest;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author joey
 */
public class MyVaadinUITest {
    
    public MyVaadinUITest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }

    /**
     * Test of init method, of class MyVaadinUI.
     */
    @Test
    public void testInit() {
        System.out.println("init");
        VaadinRequest request = null;
        MyVaadinUI instance = new MyVaadinUI();
        instance.init(request);
        // TODO review the generated test code and remove the default call to fail.
        assertTrue(true);
    }
    
}
