package sourcehash

import (
	"bytes"
	"crypto/sha256"
	"encoding/hex"
	"fmt"
	"io/fs"
	"os"
	"path/filepath"
	"sort"
	"strings"
)

func Compute(repositoryRoot string) (string, error) {
	toolRoot := filepath.Join(repositoryRoot, "tools", "task-lint")
	var paths []string

	err := filepath.WalkDir(toolRoot, func(path string, entry fs.DirEntry, err error) error {
		if err != nil {
			return err
		}
		if entry.IsDir() {
			if entry.Name() == "bin" {
				return filepath.SkipDir
			}
			return nil
		}
		if shouldInclude(entry.Name()) {
			paths = append(paths, path)
		}
		return nil
	})
	if err != nil {
		return "", fmt.Errorf("collect task-lint sources: %w", err)
	}
	sort.Strings(paths)

	hash := sha256.New()
	for _, path := range paths {
		relativePath, err := filepath.Rel(repositoryRoot, path)
		if err != nil {
			return "", err
		}
		if _, err := hash.Write([]byte(filepath.ToSlash(relativePath))); err != nil {
			return "", err
		}
		if _, err := hash.Write([]byte{0}); err != nil {
			return "", err
		}

		content, err := os.ReadFile(path)
		if err != nil {
			return "", err
		}
		content = bytes.ReplaceAll(content, []byte("\r\n"), []byte("\n"))
		content = bytes.ReplaceAll(content, []byte("\r"), []byte("\n"))
		hash.Write(content)
		if _, err := hash.Write([]byte{0}); err != nil {
			return "", err
		}
	}

	return hex.EncodeToString(hash.Sum(nil)), nil
}

func shouldInclude(name string) bool {
	return strings.HasSuffix(name, ".go") ||
		name == "go.mod" ||
		name == "go.sum" ||
		name == "Dockerfile"
}
