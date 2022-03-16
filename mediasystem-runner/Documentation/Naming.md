
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

Attempts to find something. A search can fail, and as that is an expected outcome, 
it will not throw an exception to indicate there was no result. Instead it will 
return `null`, an empty `Optional` or an empty collection type to indicate there 
was no result.

Exceptions can only be thrown when the search itself could not be performed.

Q: "Find my watch" A: "I looked everywhere, but I couldn't find it".

**`get~`**

Gets a value that is known to exist. No failure is expected, and so any failure
should throw an exception. A getter can return `null` when that is an actual valid
value, but it cannot use it to indicate a value could not be obtained -- it must
throw an exception for those  cases. Similarly, it can return an `Optional`, but
again, an empty `Optional` does not signify the value was not found, but instead
signifies that absence of the value which is a valid state. The same applies for
getters returning a collection type. If an empty collection is returned, it is a
valid state and does not signify failure of the getter in any way.

Q: "Get me the salt from the table" A: "Here it is"
