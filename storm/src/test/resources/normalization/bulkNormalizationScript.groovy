Map<String, Object> result = new HashMap<>();
binding.getVariables();
Map<String, Object> defaultValues = ['foo': 'bar', 'new-field': 'new-value'];
__outputSchema.getFields().each
        { field ->
            String key = field.getName();
            if (binding.hasVariable(key)) {
                result.put(key, binding.getVariable(key));
            } else if (defaultValues.containsKey(key)) {
                result.put(key, defaultValues.get(key));
            }
        };
/* assuming 'temp' field is always there, if it does not exists it will throw an error */
result.put('temperature', new Float((temp - 32) * 5 / 9f));
return result;