function succ (x) 
   return x + 1
end

function apply (f, x)
   return f (x)
end

y = apply(succ, 0)
