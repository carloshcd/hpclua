function swap (x, y)
   return y, x
end

function pair (x, y) 
   return x, y 
end

function swappair (x,y)
   return swap (pair (x, y))
end
