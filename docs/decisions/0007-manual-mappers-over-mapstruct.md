# ADR-0007: Use manual mapper methods, not MapStruct

**Status:** Accepted
**Date:** 2026-09-12
**Phase:** 4

## Context

Phase 1 (§6.2) established that controllers never return JPA entities directly — every response crosses the controller boundary as a DTO. Phase 1 left the specific mapping mechanism ("MapStruct or manual mappers") as an open decision for Phase 4, once real entities and DTOs existed to map between.

## Options considered

1. **MapStruct** (annotation-processor-generated mapper implementations).
   - Pros: eliminates boilerplate for large entity/DTO graphs; compile-time-checked field mapping; widely used in Java enterprise codebases.
   - Cons: adds an annotation-processing step to the build that's a common source of "works on my machine" friction (stale generated sources, IDE annotation-processing configuration, processor version alignment with the Java/Lombok toolchain if Lombok is ever added); for entities this small (a handful of fields, no deep object graphs), the generated code isn't meaningfully shorter or safer than writing it directly; harder to attach mapping-specific business logic (e.g. "only include category if the user has visibility") without escaping into custom methods anyway.
2. **Manual mapper methods** — a plain static (or instance) method per entity→DTO conversion, in a small mapper class per feature.
   - Pros: zero build-time magic — what you read is what runs, which matters when debugging a mapping issue at 11pm; no annotation-processing configuration to get right; trivially easy to explain in an interview, since there's no generated code to reason about; naturally accommodates the odd bit of extra logic a mapping needs (e.g. `CategoryMapper.toResponse` deciding what to expose) without fighting a code generator.
   - Cons: more typing for entities with many fields; no compile-time enforcement that every field was mapped (a genuinely useful MapStruct property) — mitigated here by keeping DTOs small and reviewing new mappers as part of normal PR review.

## Decision

Use plain manual mapper classes/methods (e.g. `CategoryMapper.toResponse(IncidentCategory)`), one per feature package, rather than MapStruct. The MapStruct dependencies added speculatively in Phase 2's `pom.xml` are removed as part of this decision — keeping the dependency tree matched to what's actually used, rather than carrying an unused library "just in case."

## Consequences

- No annotation-processing step in the Maven build for mapping — one less thing that can silently go stale.
- Adding a new DTO means writing a small, explicit mapper method — slightly more code per entity, judged a reasonable tradeoff at this project's scale (a handful of entities, not dozens).
- If the entity/DTO surface grows substantially in a later phase and manual mapping becomes genuinely repetitive and error-prone, re-adopting MapStruct is a contained, reversible change — this decision is revisited if that happens, not treated as permanent on principle.
