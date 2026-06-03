#!/usr/bin/env python3
"""Fix checkstyle violations WITHOUT breaking Java syntax."""
import re
from pathlib import Path

SRC = Path(__file__).parent / "src/main/java"

LOMBOK_ANNOTATIONS = {
    'Getter', 'Setter', 'Builder', 'NoArgsConstructor', 'AllArgsConstructor',
    'Data', 'ToString', 'EqualsAndHashCode', 'RequiredArgsConstructor',
    'Slf4j', 'SuperBuilder', 'AccessLevel',
}

SPRING_WEB = {
    'RestController', 'RequestMapping', 'GetMapping', 'PostMapping',
    'PutMapping', 'DeleteMapping', 'PatchMapping', 'PathVariable',
    'RequestBody', 'RequestParam', 'ResponseStatus', 'CrossOrigin',
}


def _find_control_statement(line: str):
    """Return (indent, condition, body) if line is a brace-less control statement."""
    stripped = line.rstrip()
    if '{' in stripped or not stripped.endswith(';'):
        return None
    m = re.match(r'^(\s*)((?:else\s+if|if|else|for|while)\b.*)\s+(.+);\s*$', stripped)
    if not m:
        return None
    indent, condition, body = m.group(1), m.group(2), m.group(3)
    if body.startswith('{'):
        return None
    return indent, condition, body


def fix_need_braces(content: str) -> str:
    lines = content.split('\n')
    result = []
    for line in lines:
        parsed = _find_control_statement(line)
        if parsed:
            indent, condition, body = parsed
            result.append(f'{indent}{condition} {{')
            result.append(f'{indent}    {body};')
            result.append(f'{indent}}}')
        else:
            result.append(line)
    return '\n'.join(result)


def fix_star_imports(content: str, filepath: Path) -> str:
    star_imports = re.findall(r'^import\s+([\w.]+)\.\*;\s*$', content, re.MULTILINE)
    if not star_imports:
        return content

    for pkg in star_imports:
        if pkg == 'lombok':
            used = {a for a in LOMBOK_ANNOTATIONS if re.search(rf'@{a}\b', content)}
            if used:
                new_imports = '\n'.join(f'import {pkg}.{a};' for a in sorted(used))
                content = re.sub(rf'import\s+{re.escape(pkg)}\.\*;\s*\n', new_imports + '\n', content)
            else:
                content = re.sub(rf'import\s+{re.escape(pkg)}\.\*;\s*\n', '', content)
        elif pkg == 'org.springframework.web.bind.annotation':
            used = {a for a in SPRING_WEB if re.search(rf'@{a}\b', content)}
            if used:
                new_imports = '\n'.join(f'import {pkg}.{a};' for a in sorted(used))
                content = re.sub(rf'import\s+{re.escape(pkg)}\.\*;\s*\n', new_imports + '\n', content)
        elif pkg == 'java.util':
            java_util = [
                'List', 'Map', 'Set', 'HashMap', 'HashSet', 'ArrayList', 'LinkedHashMap',
                'Collections', 'Comparator', 'Optional', 'UUID', 'Objects', 'Stream',
                'Collectors', 'Iterator', 'LinkedList', 'TreeMap', 'TreeSet', 'Arrays',
            ]
            used = {t for t in java_util if re.search(rf'\b{t}\b', content)}
            if used:
                new_imports = '\n'.join(f'import {pkg}.{t};' for t in sorted(used))
                content = re.sub(rf'import\s+{re.escape(pkg)}\.\*;\s*\n', new_imports + '\n', content)
        else:
            search_dir = SRC
            for part in pkg.split('.'):
                candidate = search_dir / part
                if candidate.is_dir():
                    search_dir = candidate
                else:
                    break
            if search_dir.is_dir():
                types = [f.stem for f in search_dir.glob('*.java')]
                used = {t for t in types if re.search(rf'\b{t}\b', content)}
                if used:
                    new_imports = '\n'.join(f'import {pkg}.{t};' for t in sorted(used))
                    content = re.sub(rf'import\s+{re.escape(pkg)}\.\*;\s*\n', new_imports + '\n', content)
    return content


def remove_unused_imports(content: str) -> str:
    imports = re.findall(r'^import\s+(?:static\s+)?([\w.]+)\.(\w+);\s*$', content, re.MULTILINE)
    if not imports:
        return content
    body = re.sub(r'^import\s+.*;\s*\n', '', content, flags=re.MULTILINE)
    body = re.sub(r'^package\s+.*;\s*\n', '', body, flags=re.MULTILINE)
    for full_pkg, simple in imports:
        if not re.search(rf'\b{re.escape(simple)}\b', body):
            content = re.sub(
                rf'^import\s+(?:static\s+)?{re.escape(full_pkg)}\.{re.escape(simple)};\s*\n',
                '', content, flags=re.MULTILINE)
    return content


def fix_parameter_names(content: str) -> str:
    return re.sub(r'\bUUID\s+Id\b', 'UUID id', content)


def process_file(filepath: Path) -> bool:
    original = filepath.read_text(encoding='utf-8')
    content = original
    content = fix_star_imports(content, filepath)
    content = fix_need_braces(content)
    content = fix_parameter_names(content)
    content = remove_unused_imports(content)
    if content != original:
        filepath.write_text(content, encoding='utf-8')
        return True
    return False


def main():
    changed = 0
    for java_file in sorted(SRC.rglob('*.java')):
        if process_file(java_file):
            changed += 1
            print(f'Fixed: {java_file.relative_to(SRC.parent.parent)}')
    print(f'\nTotal files changed: {changed}')


if __name__ == '__main__':
    main()
