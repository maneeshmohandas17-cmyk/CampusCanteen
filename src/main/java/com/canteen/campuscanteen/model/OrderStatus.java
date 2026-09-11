package com.canteen.campuscanteen.model;

public enum OrderStatus {
    AWAITING_PAYMENT("Awaiting Online Payment", "badge-info", "Complete the online payment to confirm and reserve your order."),
    CONFIRMED("Reserved at Counter", "badge-warning", "Your prepared meal is reserved at the counter under your Token."),
    READY_FOR_PICKUP("Ready for Counter Pickup", "badge-success", "Food is packed and ready at the counter! Show Token to collect."),
    COMPLETED("Collected & Handed Over", "badge-secondary", "Food has been handed over to the student."),
    CANCELLED("Cancelled", "badge-danger", "Order was cancelled.");

    private final String displayName;
    private final String badgeClass;
    private final String description;

    OrderStatus(String displayName, String badgeClass, String description) {
        this.displayName = displayName;
        this.badgeClass = badgeClass;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getBadgeClass() {
        return badgeClass;
    }

    public String getDescription() {
        return description;
    }
}
