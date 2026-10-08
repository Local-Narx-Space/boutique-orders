# Scoped security exception

CVE-2026-47884 affects XsltView with a wildcard handler and implicit view name. This application has only JSON REST controllers, no XSLT templates, no XsltView configuration and no view-rendering handler. The latest available Spring 6.2 version at validation time was 6.2.19; the feed lists the fixed Spring 7 version. The exception is scoped to this one CVE and expires 2026-11-08. Reassess immediately if view rendering is added; replace the exception with an upstream fixed 6.2 release when available. It is not a general suppression of Spring vulnerabilities.
