# Order System Refactor - Quick Implementation Checklist

## 🚨 IMMEDIATE ACTIONS (Do These First)

### 1. Clean Your Database
Choose ONE option (easiest to hardest):

**Option A: Delete Database (Easiest - Dev only)**
```bash
# In MySQL Workbench or terminal:
DROP DATABASE closetly;

# Then restart Spring Boot - it will auto-create tables correctly
# Make sure spring.jpa.hibernate.ddl-auto=update in application.properties
```

**Option B: Run Migration SQL (Better for Production)**
```bash
# In MySQL terminal or Workbench:
USE closetly;

-- Drop old columns that are causing the error
ALTER TABLE orders DROP FOREIGN KEY orders_ibfk_2;
ALTER TABLE orders DROP COLUMN IF EXISTS product_id;
ALTER TABLE orders DROP COLUMN IF EXISTS price;
```

**Option C: Use Migration File**
```bash
# Copy the migration file to your database:
# File: src/main/resources/db-migration-orders-refactor.sql
# Execute it in MySQL
```

### 2. Verify Application Properties
Check `src/main/resources/application.properties`:
```properties
spring.jpa.hibernate.ddl-auto=update  # Should be "update" (allows schema changes)
```

### 3. Restart Spring Boot
```bash
# Stop the server (Ctrl+C)
# Clear Maven cache
mvn clean

# Start server again
mvn spring-boot:run
```

### 4. Test the placeOrder Endpoint
```bash
# POST /api/orders/place
curl -X POST http://localhost:8080/api/orders/place \
  -H "Authorization: Bearer <your_jwt_token>" \
  -H "Content-Type: application/json"
```

## ✅ What Changed (Technical Summary)

| Component | Change | Status |
|-----------|--------|--------|
| **Order.java** | Removed product_id & price columns | ✅ Done |
| **OrderItem.java** | Enhanced with rentals support | ✅ Done |
| **OrderService.java** | placeOrder() handles cart → order conversion | ✅ Done |
| **OrderRepository.java** | Added eager-loading queries | ✅ Done |
| **OrderItemRepository.java** | Added utility queries | ✅ Done |
| **Database Schema** | Need to drop old columns | 🔧 Do This |

## 🧪 Testing Scenarios

### Test 1: Create Order from Cart with Multiple Items
1. Add 2+ items to cart
2. Call placeOrder endpoint
3. Expected: Single Order with multiple OrderItems

### Test 2: Price History
1. Create order with product priced at $50
2. Change product price to $100
3. Verify old order still shows $50 (not $100)

### Test 3: Rental Orders
1. Add rental item with dates to cart
2. Create order
3. Verify startDate, endDate, and rentalDays are saved

### Test 4: Fetch Order Details
1. Create order
2. Fetch using GET /api/orders/{orderId}
3. Verify all OrderItems are included

## 📝 Common Issues & Fixes

### Issue 1: "Field 'product_id' doesn't have a default value"
```
✅ Solution: Run migration SQL to drop the column
   ALTER TABLE orders DROP COLUMN product_id;
```

### Issue 2: OrderItems not saving
```
✅ Solution: Check Order entity has proper cascade
   @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
```

### Issue 3: Null pointer on order.getOrderItems()
```
✅ Solution: Verify @Builder.Default on orderItems field
   @Builder.Default
   private List<OrderItem> orderItems = new ArrayList<>();
```

### Issue 4: "Cannot insert NULL into order_id"
```
✅ Solution: Set bidirectional relationship before saving
   orderItems.forEach(item -> item.setOrder(order));
```

## 📊 Entity Relationships Diagram

```
User (1) ──────────── (N) Order
                         │
                         │ 1:N
                         │
                      OrderItem (N)
                         │
                         │ N:1
                         │
                      Product (1)
```

**Key Points**:
- ✅ Order has ONE customer
- ✅ Order has MANY OrderItems
- ✅ Each OrderItem has ONE order
- ✅ Each OrderItem references ONE product
- ✅ Products can be in MANY OrderItems (same product ordered multiple times)

## 🔄 Data Flow Example

```
Input (Cart):
  CartItem { id: "uuid-1", product: { id: 10, salePrice: 50 }, quantity: 2 }
  CartItem { id: "uuid-2", product: { id: 15, rentPricePerDay: 10 }, quantity: 1, startDate: "2024-03-25", endDate: "2024-03-30" }

↓ placeOrder() processes:

Output (Order):
  Order {
    id: 1,
    customer: User { id: 5 },
    orderItems: [
      OrderItem { product: { id: 10 }, quantity: 2, unitPrice: 50, totalPrice: 100, type: BUY },
      OrderItem { product: { id: 15 }, quantity: 1, unitPrice: 10, totalPrice: 60, type: RENT, rentalDays: 6 }
    ],
    totalAmount: 160,
    totalItems: 3,
    status: PLACED
  }

CartItems are DELETED from cart_items table after order creation
```

## 🎯 Validation Checklist

After implementation, verify:

- [ ] Database column errors are gone
- [ ] Can add items to cart without errors
- [ ] placeOrder() endpoint works
- [ ] Order is created with totalAmount & totalItems calculated correctly
- [ ] OrderItems are linked to Order
- [ ] Each OrderItem has product, quantity, price info
- [ ] For rentals: startDate, endDate, rentalDays are saved
- [ ] Cart is emptied after order creation
- [ ] Old orders can still be fetched correctly
- [ ] OrderDTO response shows all items

## 💡 Tips for Success

1. **Don't skip the database cleanup**
   - Old columns MUST be removed or migration will fail
   
2. **Use eager-loading queries**
   - Always use `findByIdWithItemsAndProducts()` 
   - Avoids N+1 query problems

3. **Set cascade correctly**
   - `cascade = CascadeType.ALL` needed
   - `orphanRemoval = true` to clean up deleted items

4. **Set bidirectional relationship**
   ```java
   orderItems.forEach(item -> item.setOrder(order));
   ```
   - Required before saving

5. **Calculate totals from items**
   ```java
   totalAmount = orderItems.stream().mapToDouble(OrderItem::getTotalPrice).sum();
   totalItems = orderItems.stream().mapToInt(OrderItem::getQuantity).sum();
   ```

## 🚀 Ready to Deploy?

Run this checklist:
- [ ] All entities updated and in sync
- [ ] Database migration completed
- [ ] Spring Boot restarted successfully
- [ ] No SQL errors in logs
- [ ] placeOrder() tested with 2+ items
- [ ] Verified order_items table has correct data
- [ ] OrderDTO response includes all items
- [ ] Old orders can still be retrieved

If all checkboxes pass ✅, your refactor is complete!

---

**Need Help?**
- Check `ORDERS_DESIGN_GUIDE.md` for detailed architecture
- Review `OrderServiceImpl.java` for implementation examples
- Look at error logs: `spring.jpa.show-sql=true` shows SQL statements
