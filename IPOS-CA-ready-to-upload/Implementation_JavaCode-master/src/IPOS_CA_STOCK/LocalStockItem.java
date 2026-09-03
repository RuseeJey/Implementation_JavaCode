package IPOS_CA_STOCK;

import java.util.UUID;

public class LocalStockItem {

    private String itemID;
    private String itemName;
    private String unit;
    private String unitsInAPack;
    private double cost;
    private int quantity;
    private int minimumStockLevel;

    public LocalStockItem() {}

    public LocalStockItem(String itemID, String itemName, String unit, String unitsInAPack,
                          double cost, int quantity, int minimumStockLevel) {
        this.itemID = itemID;
        this.itemName = itemName;
        this.unit = unit;
        this.unitsInAPack = unitsInAPack;
        this.cost = cost;
        this.quantity = quantity;
        this.minimumStockLevel = minimumStockLevel;
    }

    public LocalStockItem(String itemName, double cost, double vatRate, int quantity) {
        this(UUID.randomUUID().toString(), itemName, "", "", cost, quantity, 0);
    }

    public double calculateRetailPrice() {
        return cost;
    }

    public boolean isLowStock() {
        return quantity < minimumStockLevel;
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public String getItemID()            { return itemID; }
    public String getItemName()          { return itemName; }
    public String getUnit()              { return unit; }
    public String getUnitsInAPack()      { return unitsInAPack; }
    public double getVatRate()           { return 0.0; }
    public double getCost()              { return cost; }
    public int    getQuantity()          { return quantity; }
    public int    getMinimumStockLevel() { return minimumStockLevel; }

    // Backward-compatible aliases for older code paths
    public String getPackageType()       { return ""; }
    public String getUnitType()          { return ""; }
    public int    getUnitsPerPack()      { return 1; }
    public double getWholesaleCost()     { return cost; }
    public double getMarkupRate()        { return 0.0; }
    public double calculateRetailPriceWithVAT() { return calculateRetailPrice(); }

    // ── Setters ───────────────────────────────────────────────────────────────

    public void setItemID(String v)           { itemID = v; }
    public void setItemName(String v)         { itemName = v; }
    public void setUnit(String v)             { unit = v; }
    public void setUnitsInAPack(String v)     { unitsInAPack = v; }
    public void setCost(double v)             { cost = v; }
    public void setVatRate(double v)          { }
    public void setQuantity(int v)            { quantity = v; }
    public void setMinimumStockLevel(int v)   { minimumStockLevel = v; }

    // Backward-compatible no-op setters for older code paths
    public void setPackageType(String v)      { }
    public void setUnitType(String v)         { }
    public void setUnitsPerPack(int v)        { }
    public void setWholesaleCost(double v)    { cost = v; }
    public void setMarkupRate(double v)       { }

    @Override
    public String toString() {
        return itemID + " — " + itemName + " | Qty: " + quantity +
                " | Retail: £" + String.format("%.2f", calculateRetailPrice()) +
                (isLowStock() ? " [LOW]" : "");
    }
}