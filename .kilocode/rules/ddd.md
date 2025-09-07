# Domain-Driven Design Rules

1. An aggregate is a cluster of domain objects that can be treated as a
   single unit, it must stay internally consistent after each busines operation.

   For each aggregate:
     - Name root and contained entities
     - Explain why this aggregate is sized the way it is
          (transaction size, concurrency, read/write patterns)

2. Always follow Tactical Domain-Driven Design Patterns