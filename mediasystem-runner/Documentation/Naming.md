
# Naming

## Class Naming

**`~Database`**

Provides low level database operations that operate in a transaction, typically to 
insert, update or delete an entry in a database.  Deals with annotated Record classes.

Records can be returned directly, but donot need to provided (constructed on demand).

Caching can be done here safely.

**`~Store`**

Provides application level functions, never deals with records in its API.

**`~Record`**

Annotated low level storage class intended for use by Database classes.

## Method Naming

**`find~`**

Returns an `Optional`, `List`, `Map`, `Set` or another Collection class, never returns `null`

**`get~`**

Returns a value (or `null` if allowed)
