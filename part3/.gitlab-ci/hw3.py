from sys import exit
from checks import check, check_files_exist, check_sqlite_commands, check_has_sql_comments

query_files = [f"hw3-q{q}.sql" for q in range(1, 9)]
writeup = "hw3-d.txt"

def check_file_locations(error):
  check_files_exist(error, query_files + [writeup])

def check_sqlite_commands_queries(error):
  check_sqlite_commands(error, query_files, should_have_sqlite_commands=False)

def check_query_comments(error):
  check_has_sql_comments(error, query_files)

def main():
  print("Hi, I'm a submission checker. I will perform some checks to ensure that you're submitting your files correctly.")
  print("  (note: these checks are not guaranteed to be comprehensive; please read the spec carefully)")
  print()

  num_errors = check("files are in the correct location", check_file_locations)
  if num_errors:
    exit(1)

  num_errors += check("queries don't have SQLite commands", check_sqlite_commands_queries)
  num_errors += check("queries include comments with # of rows, query time, and the first 20 rows", check_query_comments)
  if num_errors > 0:
    exit(1)

main()
