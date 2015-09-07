# star

A Clojure library to write slowly changing dimensions and fact tables for star schemas.

[![Build Status](https://circleci.com/gh/bhoflack/star.svg?style=shield&circle-token=:circle-token)](https://circleci.com/gh/bhoflack/star/tree/master)

## Usage

First import the library.
> (use 'star.core)

Define your datasource.
> (def ds {:subprotocol "postgresql"
>          :classname "org.postgresql.Driver"
>          :subname "//localhost/star"
>          :user "star"
>          :password "star"})

This datasource will be used to store your star schema.

Now you can write to the slowly changing dimension.  A call to slowly-changing-dimension will only write to the dimension table when there isn't a entry with the same values for the specified keys.

> (slowly-changing-dimension ds
>                            :dim_ci
>                            {:memory 2048 :hostname "test" :num_cpus 2}
>                            [:hostname :memory :num_cpus]
>                            [:hostname])

This will return the id for the row with specified hostname,  memory and num_cpus if it exists ( and isn't stopped ).  If the active row for the specified hostname contains different values,  it will stop that row,  and create a new one with the specified values.

This call will always return an id pointing to the row with the specified keys.

After saving all slowly-changing-dimensions,  we can create a fact table that points to those slowly-changing-dimensions:

> (insert-into-facttable ds
>                        :fact_values
>                        { :id (random-uuid)
>                          :ci_id ci
>                          :value 20
>                        })

Now you can query your data.

## License

Copyright © 2015 Brecht Hoflack

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
