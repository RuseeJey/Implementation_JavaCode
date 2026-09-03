package IPOS_CA_ORD;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OrderItemTest {

    @Test
    void constructorShouldSetFieldsCorrectly() {
        OrderItem item = new OrderItem("10000001", 5, 0.10);

        assertEquals("10000001", item.getItemID());
        assertEquals(5, item.getQuantity());
        assertEquals(0.10, item.getUnitCost(), 0.001);
    }

    @Test
    void getTotalLineCostShouldReturnQuantityTimesUnitCost() {
        OrderItem item = new OrderItem("30000002", 20, 15.00);

        assertEquals(300.00, item.getTotalLineCost(), 0.001);
    }

    @Test
    void getTotalLineCostShouldWorkForSmallDecimalValues() {
        OrderItem item = new OrderItem("10000001", 10, 0.10);

        assertEquals(1.00, item.getTotalLineCost(), 0.001);
    }

    @Test
    void getTotalLineCostShouldReturnZeroWhenQuantityIsZero() {
        OrderItem item = new OrderItem("10000003", 0, 1.20);

        assertEquals(0.0, item.getTotalLineCost(), 0.001);
    }

    @Test
    void gettersShouldReturnOriginalValues() {
        OrderItem item = new OrderItem("20000005", 2, 2.50);

        assertEquals("20000005", item.getItemID());
        assertEquals(2, item.getQuantity());
        assertEquals(2.50, item.getUnitCost(), 0.001);
    }
}