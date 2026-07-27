package lint

import (
	"fmt"
	"io/fs"
	"os"
	"path/filepath"
	"sort"
	"strings"
)

type Error struct {
	Path    string
	Line    int
	Message string
}

func (e Error) String() string {
	return fmt.Sprintf("%s:%d: %s", filepath.ToSlash(e.Path), e.Line, e.Message)
}

type requiredSection struct {
	Name string
	Line int
}

func Run(repositoryRoot string) ([]Error, error) {
	standardPath := filepath.Join(repositoryRoot, "tasks", "STANDARD.md")
	required, standardErrors, err := loadRequiredSections(repositoryRoot, standardPath)
	if err != nil {
		return nil, err
	}
	if len(standardErrors) > 0 {
		return standardErrors, nil
	}

	var paths []string
	tasksRoot := filepath.Join(repositoryRoot, "tasks")
	statusDirectories, err := os.ReadDir(tasksRoot)
	if err != nil {
		return nil, fmt.Errorf("read tasks directory %s: %w", tasksRoot, err)
	}
	for _, entry := range statusDirectories {
		if !entry.IsDir() || strings.HasPrefix(entry.Name(), ".") {
			continue
		}
		if err := collectMarkdownFiles(
			filepath.Join(tasksRoot, entry.Name()),
			&paths,
		); err != nil {
			return nil, err
		}
	}
	sort.Strings(paths)

	var errors []Error
	for _, path := range paths {
		documentErrors, err := lintDocument(repositoryRoot, path, required)
		if err != nil {
			return nil, err
		}
		errors = append(errors, documentErrors...)
	}
	return errors, nil
}

func loadRequiredSections(
	repositoryRoot string,
	standardPath string,
) ([]requiredSection, []Error, error) {
	sections, definitionLines, err := parseRequiredSectionDefinitions(standardPath)
	if err != nil {
		return nil, nil, fmt.Errorf("read task standard: %w", err)
	}

	relativePath, err := filepath.Rel(repositoryRoot, standardPath)
	if err != nil {
		return nil, nil, err
	}

	if len(definitionLines) == 0 {
		return nil, []Error{{
			Path:    relativePath,
			Line:    1,
			Message: "'## 필수 섹션' heading이 없습니다",
		}}, nil
	}
	if len(definitionLines) > 1 {
		return nil, []Error{{
			Path: relativePath,
			Line: definitionLines[1],
			Message: fmt.Sprintf(
				"'## 필수 섹션' heading이 중복되었습니다 (첫 번째 위치: %d줄)",
				definitionLines[0],
			),
		}}, nil
	}
	if len(sections) == 0 {
		return nil, []Error{{
			Path:    relativePath,
			Line:    definitionLines[0],
			Message: "'필수 섹션' 절에 번호가 붙은 '###' heading이 없습니다",
		}}, nil
	}

	seen := make(map[string]int)
	var required []requiredSection
	var errors []Error
	for _, item := range sections {
		if firstLine, exists := seen[item.Name]; exists {
			errors = append(errors, Error{
				Path: relativePath,
				Line: item.Line,
				Message: fmt.Sprintf(
					"필수 섹션 '%s'이 중복되었습니다 (첫 번째 위치: %d줄)",
					item.Name,
					firstLine,
				),
			})
			continue
		}
		seen[item.Name] = item.Line
		required = append(required, requiredSection{Name: item.Name, Line: item.Line})
	}
	return required, errors, nil
}

func collectMarkdownFiles(root string, paths *[]string) error {
	info, err := os.Stat(root)
	if err != nil {
		return fmt.Errorf("read task directory %s: %w", root, err)
	}
	if !info.IsDir() {
		return fmt.Errorf("task path is not a directory: %s", root)
	}

	return filepath.WalkDir(root, func(path string, entry fs.DirEntry, err error) error {
		if err != nil {
			return err
		}
		if entry.Type().IsRegular() &&
			strings.EqualFold(filepath.Ext(entry.Name()), ".md") {
			*paths = append(*paths, path)
		}
		return nil
	})
}

func lintDocument(
	repositoryRoot string,
	path string,
	required []requiredSection,
) ([]Error, error) {
	sections, err := parseSections(path)
	if err != nil {
		return nil, fmt.Errorf("read task document %s: %w", path, err)
	}

	relativePath, err := filepath.Rel(repositoryRoot, path)
	if err != nil {
		return nil, err
	}

	byName := make(map[string][]section)
	for _, item := range sections {
		byName[item.Name] = append(byName[item.Name], item)
	}

	var errors []Error
	positions := make(map[string]int)
	for _, expected := range required {
		matches := byName[expected.Name]
		switch len(matches) {
		case 0:
			errors = append(errors, Error{
				Path:    relativePath,
				Line:    1,
				Message: fmt.Sprintf("필수 섹션 '%s'이 없습니다", expected.Name),
			})
		case 1:
			positions[expected.Name] = matches[0].Line
			if matches[0].Content == "" {
				errors = append(errors, Error{
					Path:    relativePath,
					Line:    matches[0].Line,
					Message: fmt.Sprintf("필수 섹션 '%s'의 내용이 비어 있습니다", expected.Name),
				})
			}
		default:
			for _, duplicate := range matches[1:] {
				errors = append(errors, Error{
					Path: relativePath,
					Line: duplicate.Line,
					Message: fmt.Sprintf(
						"필수 섹션 '%s'이 중복되었습니다 (첫 번째 위치: %d줄)",
						expected.Name,
						matches[0].Line,
					),
				})
			}
		}
	}

	lastLine := 0
	lastName := ""
	for _, expected := range required {
		line, exists := positions[expected.Name]
		if !exists {
			continue
		}
		if line < lastLine {
			errors = append(errors, Error{
				Path: relativePath,
				Line: line,
				Message: fmt.Sprintf(
					"필수 섹션 '%s'은 '%s' 뒤에 있어야 합니다",
					expected.Name,
					lastName,
				),
			})
		}
		if line > lastLine {
			lastLine = line
			lastName = expected.Name
		}
	}

	sort.SliceStable(errors, func(i, j int) bool {
		if errors[i].Path != errors[j].Path {
			return errors[i].Path < errors[j].Path
		}
		return errors[i].Line < errors[j].Line
	})
	return errors, nil
}
