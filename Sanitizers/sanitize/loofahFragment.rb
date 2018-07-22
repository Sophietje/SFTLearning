require 'loofah'
puts Loofah.scrub_fragment(ARGV[0], :prune)