import sys
from os import path
from termcolor import cprint

sqlite_commands = [
  ".backup", ".bail", ".databases", ".dump", ".echo", ".exit", ".explain", ".header(s)", ".help", ".import",
  ".indices", ".load", ".log", ".mode", ".nullvalue", ".output", ".output", ".print", ".prompt", ".quit", ".read",
  ".schema", ".separator", ".show", ".stats", ".tables", ".timeout", ".width", ".timer"
]

def check(description, checker):
  num_errors = 0

  def print_error(*message):
    nonlocal num_errors
    joined_message = " ".join(["        ‣ ", *message])
    cprint(joined_message, "red")
    num_errors += 1

  print(f"• Checking that {description}")
  checker(print_error)

  if num_errors:
    cprint(f"    {num_errors} errors", "white", "on_red")
  else:
    cprint(f"    {num_errors} errors", "green")
  print()

  return num_errors

def check_files_exist(error, files):
  not_found_files = [file for file in files if not path.exists(file)]
  for file in not_found_files:
    error(file, "not found")

def check_sqlite_commands(error, files, should_have_sqlite_commands):
  bad_files = [file for file in files if file_has_sqlite_commands(file) != should_have_sqlite_commands]
  for file in bad_files:
    if should_have_sqlite_commands:
      error(file, "should contain SQLite commands (.mode, etc...)")
    else:
      error(file, "should not contain SQLite commands (.mode, etc...)")

def check_has_sql_comments(error, files):
  bad_files = [file for file in files if not file_has_sql_comments(file)]
  for file in bad_files:
    error(file, "doesn't have comments")

def file_has_sql_comments(file_path):
  file_contents = open(file_path, "r").read()
  return "/*" in file_contents or "--" in file_contents

def file_has_sqlite_commands(file_path):
  file_contents = open(file_path, "r").read()
  return any(filter(lambda l: not l.lstrip().startswith("--"), [command in file_contents for command in sqlite_commands]))
