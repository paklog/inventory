# Spring best practices

## Implementation Patterns and Design

1. Always inject dependencies via constructor
2. Using @Service instead of @Component
3. Minimize visibility of your components by using `private` where possible, and only expose what’s truly necessary as public.
4. Write unit tests for your business logic using dependency injection and pure functions to make testing straightforward and independent from Spring’s context
5. Use SRP
