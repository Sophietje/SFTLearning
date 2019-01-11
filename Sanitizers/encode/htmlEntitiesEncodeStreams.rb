require 'htmlentities'
coder = HTMLEntities.new
ARGF.each_line do |line|
  puts coder.encode(line)
  STDOUT.flush
end