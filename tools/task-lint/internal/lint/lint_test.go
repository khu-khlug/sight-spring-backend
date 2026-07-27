package lint

import (
	"os"
	"path/filepath"
	"strings"
	"testing"
)

func TestRunAcceptsValidDocumentsWithLFAndCRLF(t *testing.T) {
	root := createRepository(t, defaultStandard)
	writeTask(t, root, "open/lf.md", validDocument("\n"))
	writeTask(t, root, "completed/crlf.md", validDocument("\r\n"))
	writeTask(t, root, "cancelled/nested/cancelled.md", validDocument("\n"))

	errors, err := Run(root)
	if err != nil {
		t.Fatal(err)
	}
	if len(errors) != 0 {
		t.Fatalf("expected no errors, got: %v", errors)
	}
}

func TestRunReportsMissingDuplicateOutOfOrderAndEmptySections(t *testing.T) {
	root := createRepository(t, defaultStandard)
	writeTask(t, root, "open/invalid.md", strings.Join([]string{
		"# Invalid",
		"",
		"## Database",
		"",
		"value",
		"",
		"## Overview",
		"",
		"   ",
		"",
		"## Database",
		"",
		"duplicate",
	}, "\n"))

	errors, err := Run(root)
	if err != nil {
		t.Fatal(err)
	}

	messages := errorMessages(errors)
	for _, expected := range []string{
		"필수 섹션 'Overview'의 내용이 비어 있습니다",
		"필수 섹션 'Behavior'이 없습니다",
		"필수 섹션 'Database'이 중복되었습니다",
	} {
		if !strings.Contains(messages, expected) {
			t.Errorf("expected %q in errors:\n%s", expected, messages)
		}
	}
}

func TestRunChecksRequiredSectionOrderAndAllowsAdditionalSections(t *testing.T) {
	root := createRepository(t, defaultStandard)
	writeTask(t, root, "open/order.md", strings.Join([]string{
		"# Order",
		"",
		"## Behavior",
		"",
		"value",
		"",
		"## Additional",
		"",
		"value",
		"",
		"## Overview",
		"",
		"value",
		"",
		"## Database",
		"",
		"value",
	}, "\n"))

	errors, err := Run(root)
	if err != nil {
		t.Fatal(err)
	}
	if len(errors) != 1 ||
		!strings.Contains(errors[0].Message, "'Behavior'은 'Overview' 뒤에 있어야 합니다") {
		t.Fatalf("expected one order error, got: %v", errors)
	}
}

func TestRunIgnoresHeadingsInsideFencedCodeBlocks(t *testing.T) {
	root := createRepository(t, defaultStandard)
	document := validDocument("\n") + strings.Join([]string{
		"",
		"## Additional",
		"",
		"```markdown",
		"## Overview",
		"```",
	}, "\n")
	writeTask(t, root, "open/fence.md", document)

	errors, err := Run(root)
	if err != nil {
		t.Fatal(err)
	}
	if len(errors) != 0 {
		t.Fatalf("expected no errors, got: %v", errors)
	}
}

func TestRunRejectsDuplicateSectionsInStandard(t *testing.T) {
	root := createRepository(t, `# Standard

## 필수 섹션

### 1. Overview

### 2. Overview
`)

	errors, err := Run(root)
	if err != nil {
		t.Fatal(err)
	}
	if len(errors) != 1 ||
		!strings.Contains(errors[0].Message, "필수 섹션 'Overview'이 중복되었습니다") {
		t.Fatalf("expected standard duplicate error, got: %v", errors)
	}
}

func TestRunRejectsDuplicateRequiredSectionBlocksInStandard(t *testing.T) {
	root := createRepository(t, `# Standard

## 필수 섹션

### 1. Overview

## Other

## 필수 섹션

### 2. Behavior
`)

	errors, err := Run(root)
	if err != nil {
		t.Fatal(err)
	}
	if len(errors) != 1 ||
		!strings.Contains(errors[0].Message, "'## 필수 섹션' heading이 중복되었습니다") {
		t.Fatalf("expected duplicate definition block error, got: %v", errors)
	}
}

const defaultStandard = `# Standard

## Introduction

### Not a required section

## 필수 섹션

### 1. Overview

description

### 2. Behavior

description

### 3. Database

description

## Other

### Not a required section
`

func validDocument(newline string) string {
	lines := []string{
		"# Valid",
		"",
		"## Overview",
		"",
		"overview",
		"",
		"## Behavior",
		"",
		"behavior",
		"",
		"## Database",
		"",
		"database",
		"",
	}
	return strings.Join(lines, newline)
}

func createRepository(t *testing.T, standard string) string {
	t.Helper()
	root := t.TempDir()
	for _, directory := range []string{
		filepath.Join(root, "tasks", "open"),
		filepath.Join(root, "tasks", "completed"),
	} {
		if err := os.MkdirAll(directory, 0o755); err != nil {
			t.Fatal(err)
		}
	}
	if err := os.WriteFile(
		filepath.Join(root, "tasks", "STANDARD.md"),
		[]byte(standard),
		0o644,
	); err != nil {
		t.Fatal(err)
	}
	return root
}

func writeTask(t *testing.T, root string, relativePath string, content string) {
	t.Helper()
	path := filepath.Join(root, "tasks", relativePath)
	if err := os.MkdirAll(filepath.Dir(path), 0o755); err != nil {
		t.Fatal(err)
	}
	if err := os.WriteFile(path, []byte(content), 0o644); err != nil {
		t.Fatal(err)
	}
}

func errorMessages(errors []Error) string {
	var messages []string
	for _, item := range errors {
		messages = append(messages, item.String())
	}
	return strings.Join(messages, "\n")
}
