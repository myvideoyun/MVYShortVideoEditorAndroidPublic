# Mainly to encourage writing up some reasoning about the PR, rather than
# just leaving a title
if github.pr_body.length < 5
  warn "Please provide a summary in the Pull Request description"
end

# Warn when there is a big PR
if git.lines_of_code > 500
    warn("Big PR")
end

# Ensure a clean commits history
if git.commits.any? { |c| c.message =~ /^Merge branch '#{github.branch_for_base}'/ }
  warn "Please rebase to get rid of the merge commits in this PR"
end

# If these are all empty something has gone wrong, better to raise it in a comment
if git.modified_files.empty? && git.added_files.empty? && git.deleted_files.empty?
  warn "This PR has no changes at all, this is likely an issue during development."
end
