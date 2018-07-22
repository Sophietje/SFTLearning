require 'htmlentities'
coder = HTMLEntities.new
puts coder.decode(ARGV[0])
