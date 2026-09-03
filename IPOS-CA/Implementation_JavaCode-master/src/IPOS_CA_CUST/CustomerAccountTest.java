package IPOS_CA_CUST;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CustomerAccountTest {

    @Test
    void constructorWithThreeParametersShouldSetDefaultValuesCorrectly() {
        CustomerAccount account = new CustomerAccount(
                "Eva Bauyer",
                "1, Liverpool Street, London EC2V 8NS",
                500.0
        );

        assertEquals("Eva Bauyer",                          account.getName());
        assertEquals("1, Liverpool Street, London EC2V 8NS", account.getAddress());
        assertEquals(500.0,    account.getCreditLimit(),    0.001);
        assertEquals(0.0,      account.getCurrentBalance(), 0.001);
        assertEquals("normal", account.getAccountStatus());
        assertNotNull(account.getCustomerID());
        assertFalse(account.getCustomerID().isEmpty());
    }

    @Test
    void constructorWithSixParametersShouldSetAllFieldsCorrectly() {
        CustomerAccount account = new CustomerAccount(
                "ACC0001",
                "Eva Bauyer",
                "1, Liverpool Street, London EC2V 8NS",
                500.0,
                0.0,
                "normal"
        );

        assertEquals("ACC0001",  account.getCustomerID());
        assertEquals("Eva Bauyer", account.getName());
        assertEquals("1, Liverpool Street, London EC2V 8NS", account.getAddress());
        assertEquals(500.0,    account.getCreditLimit(),    0.001);
        assertEquals(0.0,      account.getCurrentBalance(), 0.001);
        assertEquals("normal", account.getAccountStatus());
    }

    @Test
    void setCreditLimitShouldUpdateCreditLimit() {
        CustomerAccount account = new CustomerAccount();

        account.setCreditLimit(500.0);

        assertEquals(500.0, account.getCreditLimit(), 0.001);
    }

    @Test
    void setCurrentBalanceShouldUpdateBalance() {
        CustomerAccount account = new CustomerAccount();

        account.setCurrentBalance(125.50);

        assertEquals(125.50, account.getCurrentBalance(), 0.001);
    }

    @Test
    void setAccountStatusShouldUpdateStatus() {
        CustomerAccount account = new CustomerAccount();

        account.setAccountStatus("suspended");

        assertEquals("suspended", account.getAccountStatus());
    }

    @Test
    void setNameAndAddressShouldUpdateCustomerDetails() {
        CustomerAccount account = new CustomerAccount();

        account.setName("Glynne Morrison");
        account.setAddress("1, Liverpool Street, London EC2V 8NS");

        assertEquals("Glynne Morrison", account.getName());
        assertEquals("1, Liverpool Street, London EC2V 8NS", account.getAddress());
    }

    @Test
    void directSettersShouldAlsoUpdateFieldsCorrectly() {
        CustomerAccount account = new CustomerAccount();

        account.setCreditLimit(750.0);
        account.setCurrentBalance(300.0);
        account.setAccountStatus("in default");

        assertEquals(750.0,        account.getCreditLimit(),    0.001);
        assertEquals(300.0,        account.getCurrentBalance(), 0.001);
        assertEquals("in default", account.getAccountStatus());
    }

    @Test
    void canMakePurchaseShouldReturnFalseWhenInDefault() {
        CustomerAccount account = new CustomerAccount();
        account.setAccountStatus("in default");

        assertFalse(account.canMakePurchase(1.0));
    }

    @Test
    void canMakePurchaseShouldReturnFalseWhenOverCreditLimit() {
        CustomerAccount account = new CustomerAccount("Eva", "London", 500.0);
        account.setCurrentBalance(450.0);

        assertFalse(account.canMakePurchase(100.0)); // 450 + 100 > 500
    }

    @Test
    void canMakePurchaseShouldReturnTrueWhenWithinLimit() {
        CustomerAccount account = new CustomerAccount("Eva", "London", 500.0);
        account.setCurrentBalance(100.0);

        assertTrue(account.canMakePurchase(50.0)); // 100 + 50 < 500
    }
}