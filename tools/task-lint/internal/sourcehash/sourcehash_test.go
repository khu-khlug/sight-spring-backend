package sourcehash

import (
	"os"
	"path/filepath"
	"testing"
)

func TestComputeChangesWhenSourceChangesAndIgnoresBinary(t *testing.T) {
	root := t.TempDir()
	toolRoot := filepath.Join(root, "tools", "task-lint")
	if err := os.MkdirAll(filepath.Join(toolRoot, "bin"), 0o755); err != nil {
		t.Fatal(err)
	}

	sourcePath := filepath.Join(toolRoot, "main.go")
	if err := os.WriteFile(sourcePath, []byte("package main\n"), 0o644); err != nil {
		t.Fatal(err)
	}
	first, err := Compute(root)
	if err != nil {
		t.Fatal(err)
	}

	if err := os.WriteFile(sourcePath, []byte("package main\n\n"), 0o644); err != nil {
		t.Fatal(err)
	}
	second, err := Compute(root)
	if err != nil {
		t.Fatal(err)
	}
	if first == second {
		t.Fatal("source change must change hash")
	}

	if err := os.WriteFile(
		filepath.Join(toolRoot, "bin", "task-lint"),
		[]byte("binary"),
		0o755,
	); err != nil {
		t.Fatal(err)
	}
	third, err := Compute(root)
	if err != nil {
		t.Fatal(err)
	}
	if second != third {
		t.Fatal("binary output must not change source hash")
	}
}

func TestComputeTreatsLFAndCRLFAsSameSource(t *testing.T) {
	root := t.TempDir()
	toolRoot := filepath.Join(root, "tools", "task-lint")
	if err := os.MkdirAll(toolRoot, 0o755); err != nil {
		t.Fatal(err)
	}

	sourcePath := filepath.Join(toolRoot, "main.go")
	if err := os.WriteFile(sourcePath, []byte("package main\n\nfunc main() {}\n"), 0o644); err != nil {
		t.Fatal(err)
	}
	lfHash, err := Compute(root)
	if err != nil {
		t.Fatal(err)
	}

	if err := os.WriteFile(
		sourcePath,
		[]byte("package main\r\n\r\nfunc main() {}\r\n"),
		0o644,
	); err != nil {
		t.Fatal(err)
	}
	crlfHash, err := Compute(root)
	if err != nil {
		t.Fatal(err)
	}

	if lfHash != crlfHash {
		t.Fatal("LF and CRLF must produce the same source hash")
	}
}
