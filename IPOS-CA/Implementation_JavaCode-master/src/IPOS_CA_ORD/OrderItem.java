package IPOS_CA_ORD;

public class OrderItem {
    private String itemID;
    private int quantity;
    private double unitCost;

    public OrderItem(String itemID, int quantity, double unitCost) {
        this.itemID = itemID;
        this.quantity = quantity;
        this.unitCost = unitCost;
    }

    public String getItemID() {
        return itemID;
    }

    public int getQuantity() {
        return quantity;
    }

    public double getUnitCost() {
        return unitCost;
    }

    public double getTotalLineCost() {
        return this.quantity * this.unitCost;
    }
}
