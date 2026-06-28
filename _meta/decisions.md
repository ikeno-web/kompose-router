# Decisions Log — kompose-router

## D-001: Kotlin 2.0+ & Compose Multiplatform
- **Status:** Accepted
- **Date:** 2026-06-28
- **Context:** Need a modern KMP navigation library.
- **Decision:** Target Kotlin 2.0.21, Compose Multiplatform 1.7.3.
- **Rationale:** Latest stable toolchain; Compose MP is production-ready across JVM/iOS/JS/Desktop.

## D-002: KMP Targets
- **Status:** Accepted
- **Date:** 2026-06-28
- **Decision:** JVM (Android), iOS (arm64/x64/simulatorArm64), JS (browser), Desktop (JVM).
- **Rationale:** Covers the full Compose MP target matrix.

## D-003: Zero Runtime Dependencies
- **Status:** Accepted
- **Date:** 2026-06-28
- **Decision:** No runtime dependencies beyond Compose Runtime/Animation/Foundation.
- **Rationale:** Minimizes version conflicts and binary size for consumers.

## D-004: Type-Safe Routes via Sealed Class/Interface
- **Status:** Accepted
- **Date:** 2026-06-28
- **Decision:** Routes defined as sealed interfaces/classes + data objects. No string-based paths.
- **Rationale:** Compile-time safety; exhaustive when-expressions; IDE autocomplete.

## D-005: DSL-Based Route Graph
- **Status:** Accepted
- **Date:** 2026-06-28
- **Decision:** `RouteGraphBuilder` with `screen<R>{}` DSL for graph definition.
- **Rationale:** Idiomatic Kotlin; composable; readable.

## D-006: No Code Generation (v1.0)
- **Status:** Accepted
- **Date:** 2026-06-28
- **Decision:** No KSP/KAPT code generation for v1.0.
- **Rationale:** Simplicity. Sealed classes + reified generics provide sufficient type safety without codegen overhead.

## D-007: Apache 2.0 License
- **Status:** Accepted
- **Date:** 2026-06-28
- **Decision:** Apache License 2.0, matching kolor-seed.
- **Rationale:** Permissive; enterprise-friendly; standard for Kotlin ecosystem.

## D-008: 90%+ Test Coverage
- **Status:** Accepted
- **Date:** 2026-06-28
- **Decision:** Target 90%+ line coverage via kotlin-test on JVM.
- **Rationale:** Library code must be highly reliable; tests also serve as documentation.
