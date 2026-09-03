package IPOS_CA_ORD;

import java.util.Date;
import java.util.List;

public class SupplierOrder {

	private String orderID;
	private Date dateOrdered;
	private String orderStatus;
	private Double totalCost;
	private List<OrderItem> items;

	public SupplierOrder(Date dateOrdered, List<OrderItem> items) {
		this.dateOrdered = dateOrdered;
		this.items = items;
		this.orderStatus = "Pending";
	}

	public void submittedOrder() {
		// TODO - implement SupplierOrder.submittedOrder
		throw new UnsupportedOperationException();
	}

	public String checkStatus() {
		// TODO - implement SupplierOrder.checkStatus
		throw new UnsupportedOperationException();
	}

	public void markAsDelivered() {
		orderStatus = "Delivered";
	}

}