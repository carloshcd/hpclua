function pFacOfN(p,n)
  if (p <= 1 or n % p ~= 0) then
    return 0
  else 
    return 1 + pFacOfN(p, n / p) 
  end
end

function map (f, a)
  local b = {}
  for k,v in pairs(a) do 
    b[k] = f(v)
  end
  return b
end  

function sieve(n)
  local t = {0} 
  for i = 2,n do 
    t[i] = 1 
  end
  local limit = n^0.5
  for i = 2,limit do 
    if (t[i]==1) then 
      for j = i*i,n,i do 
        t[j] = 0 
      end 
    end 
  end
  local p,j = {0},1
  for i = 2,n do 
    if (t[i]==1) then 
      p[j],j = i,j+1
    end 
  end
  return p
end

function filter (p) 
   return pFacOfN(p,arg[1]) 
end

primes = sieve(arg[1])
factors = map(filter, primes)
first = true
result = "The prime factorization of "..
         arg[1].." is "
if (tonumber(arg[1]) == 1) then
  result = result.."1"
else
  for k, v in pairs(factors) do
    if factors[k] ~= 0 then
      if not first then
        result = result.."*"
      else 
        first = false
      end
      result = result..primes[k]..
               "^"..factors[k]
    end
  end
end
print(result)
