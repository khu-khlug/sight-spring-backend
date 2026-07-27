package lint

import (
	"bufio"
	"fmt"
	"os"
	"strings"
)

type section struct {
	Name    string
	Line    int
	Content string
}

type fence struct {
	char  byte
	count int
}

func parseSections(path string) ([]section, error) {
	file, err := os.Open(path)
	if err != nil {
		return nil, err
	}
	defer file.Close()

	var sections []section
	var current *section
	var content []string
	var activeFence *fence

	finishCurrent := func() {
		if current == nil {
			return
		}
		current.Content = strings.TrimSpace(strings.Join(content, "\n"))
		sections = append(sections, *current)
		current = nil
		content = nil
	}

	scanner := bufio.NewScanner(file)
	scanner.Buffer(make([]byte, 64*1024), 1024*1024)
	lineNumber := 0

	for scanner.Scan() {
		lineNumber++
		line := scanner.Text()

		if marker, ok := fenceMarker(line); ok {
			if activeFence == nil {
				activeFence = &marker
			} else if marker.char == activeFence.char &&
				marker.count >= activeFence.count &&
				isClosingFence(line, marker) {
				activeFence = nil
			}

			if current != nil {
				content = append(content, line)
			}
			continue
		}

		if activeFence == nil {
			if name, ok := levelTwoHeading(line); ok {
				finishCurrent()
				current = &section{Name: name, Line: lineNumber}
				continue
			}
		}

		if current != nil {
			content = append(content, line)
		}
	}

	if err := scanner.Err(); err != nil {
		return nil, fmt.Errorf("read markdown: %w", err)
	}

	finishCurrent()
	return sections, nil
}

func levelTwoHeading(line string) (string, bool) {
	trimmed := trimUpToThreeLeadingSpaces(line)
	if !strings.HasPrefix(trimmed, "##") || strings.HasPrefix(trimmed, "###") {
		return "", false
	}
	if len(trimmed) == 2 || (trimmed[2] != ' ' && trimmed[2] != '\t') {
		return "", false
	}

	name := strings.TrimSpace(trimmed[2:])
	name = strings.TrimSpace(strings.TrimRight(name, "#"))
	if name == "" {
		return "", false
	}
	return name, true
}

func fenceMarker(line string) (fence, bool) {
	trimmed := trimUpToThreeLeadingSpaces(line)
	if len(trimmed) < 3 || (trimmed[0] != '`' && trimmed[0] != '~') {
		return fence{}, false
	}

	char := trimmed[0]
	count := 0
	for count < len(trimmed) && trimmed[count] == char {
		count++
	}
	if count < 3 {
		return fence{}, false
	}
	return fence{char: char, count: count}, true
}

func isClosingFence(line string, marker fence) bool {
	trimmed := trimUpToThreeLeadingSpaces(line)
	index := 0
	for index < len(trimmed) && trimmed[index] == marker.char {
		index++
	}
	return strings.TrimSpace(trimmed[index:]) == ""
}

func trimUpToThreeLeadingSpaces(line string) string {
	spaces := 0
	for spaces < len(line) && spaces < 3 && line[spaces] == ' ' {
		spaces++
	}
	return line[spaces:]
}
