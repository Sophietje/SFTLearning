require 'htmlentities'
coder = HTMLEntities.new
puts coder.encode(ARGV[0])
