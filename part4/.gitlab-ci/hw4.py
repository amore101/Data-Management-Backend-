from sys import exit
from checks import check, check_files_exist, check_sqlite_commands, check_has_sql_comments

sub_files = ["hw4-flightapp.pdf", "hw4-driving.sql", "hw4-ra-card.pdf", "hw4-theory.txt", "hw4-frumble.sql"]
sql_files = [f for f in sub_files if f.endswith(".sql")]

def check_file_locations(error):
  check_files_exist(error, sub_files)

def check_sqlite_commands_queries(error):
  check_sqlite_commands(error, sql_files, should_have_sqlite_commands=False)

def check_query_comments(error):
  check_has_sql_comments(error, sql_files)

def main():
  print("Hi, I'm a submission checker. I will perform some checks to ensure that you're submitting your files correctly.")
  print("  (note: these checks are not guaranteed to be comprehensive; please read the spec carefully)")
  print()

  num_errors = check("files are in the correct location", check_file_locations)
  if num_errors:
    exit(1)

  num_errors += check(".sql files don't have SQLite commands", check_sqlite_commands_queries)
  num_errors += check(".sql files include comments", check_query_comments)
  if num_errors > 0:
    exit(1)

main()
