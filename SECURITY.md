# Security Policy

## Supported Versions

| Version Range | Supported |
|--------------|----------|
| Latest       | Yes      |
| Older        | No       |

Security fixes are applied only to the latest released version. Users are strongly encouraged to upgrade.

---

## Reporting a Vulnerability

Please report security vulnerabilities via GitHub Security Advisories.

**Do not open public issues for security-related reports.**

When reporting, include:
- Affected versions
- Detailed reproduction steps
- Proof-of-concept (if available)
- Impact assessment

### Response Targets

- Initial acknowledgment: within 48 hours  
- Triage and assessment: within 3–5 days  

### Fix Targets

- Critical issues: < 7 days  
- High severity: < 14 days  
- Medium/Low: best effort  

---

## Severity Classification

- **Critical**
  - Arbitrary code execution through instrumentation
  - Unsafe class loading leading to sandbox escape

- **High**
  - Privilege escalation via agent misuse
  - Bypassing intended constraints in bytecode generation

- **Medium**
  - Denial of service via malformed bytecode
  - Stability issues impacting runtime safety

- **Low**
  - Edge-case validation issues
  - Non-exploitable incorrect behavior

---

## Disclosure Policy

- Vulnerabilities are handled via private coordination
- Fixes are developed and validated before disclosure
- Public disclosure occurs after a fix is available

Advisories will include:
- Affected versions
- Description of impact
- Mitigation steps
- Upgrade guidance

---

## Incident Response Plan

### Identification

- Incidents may be identified through vulnerability reports, dependency alerts, or community reports
- All reports received via GitHub Security Advisories are treated as potential incidents

### Assessment

- The maintainer evaluates severity using the classification above
- Critical and High issues are prioritized immediately
- Affected versions and attack surface are determined

### Containment

- If a released artifact is compromised, affected versions are flagged in the advisory
- Users are directed to pin a known-safe version or disable the affected feature (e.g., detaching a Java agent)

### Remediation

- A fix is developed and validated in a private branch
- The fix is released as a new version on Maven Central
- The GitHub Security Advisory is updated with the fixed version and mitigation steps

### Notification

- Users are notified through the GitHub Security Advisory
- Critical issues may also be announced via release notes and the project README

### Post-Incident Review

- The root cause and timeline are documented in the advisory
- Process improvements are applied to prevent recurrence

---

## Security Best Practices for Users

- Restrict use of Java agents in production environments
- Validate class loaders and transformation targets
- Keep Byte Buddy updated to the latest version
- Avoid exposing instrumentation capabilities to external users

---

## Dependencies

Byte Buddy repackages ASM internally to avoid dependency conflicts.

Users should still monitor:
- JVM-level vulnerabilities
- Build and runtime environments
- Dependency scanning results in their own systems

---

# Threat Model

## Overview

Byte Buddy is safe to use as a regular library for class generation and extension when used within trusted code. The primary risks arise from features that allow modification of existing code or execution of injected logic.

---

## High-Risk Areas

### Java Agents

Java agents can transform or redefine classes at runtime and operate with the full privileges of the hosting JVM.

**Risks**
- Injection of arbitrary code into application classes  
- Modification of security-sensitive logic  
- Full process compromise if misused or exposed  

**Guidance**
- Do not allow untrusted agents  
- Restrict agent attachment in production environments  

---

### Build Plugins

Byte Buddy can be used in build tools to modify bytecode during compilation or packaging.

**Risks**
- Execution of malicious code during build  
- Supply chain compromise via untrusted plugins or dependencies  

**Guidance**
- Only use trusted plugins and dependencies 
- Verify build integrity and dependency sources  

---

## Summary

- Regular library usage is considered safe under normal conditions  
- The main security risks stem from **code injection capabilities**, especially via agents and build-time instrumentation
