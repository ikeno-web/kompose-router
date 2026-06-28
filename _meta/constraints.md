# Constraints — kompose-router

## Technical Constraints
- **C-01:** Kotlin 2.0.21 minimum (K2 compiler)
- **C-02:** Compose Multiplatform 1.7.3 minimum
- **C-03:** Zero runtime dependencies beyond Compose Runtime, Animation, Foundation
- **C-04:** No code generation (KSP/KAPT) in v1.0
- **C-05:** Must compile for all KMP targets: JVM, iOS, JS, Desktop
- **C-06:** Navigation frame budget: <16ms (single frame at 60fps)
- **C-07:** Back stack memory: <1MB for 100 entries

## Process Constraints
- **C-08:** Apache 2.0 license
- **C-09:** 90%+ test coverage
- **C-10:** CI via GitHub Actions (JVM + JS)
- **C-11:** Follows ai_autonomous_dev_workflow v2.0
