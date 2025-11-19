package com.example.wiremock;

import com.example.models.User;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.common.FileSource;
import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.extension.ResponseDefinitionTransformer;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.RequestMethod;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class InMemoryUserTransformer extends ResponseDefinitionTransformer {

    public static final String NAME = "in-memory-user-transformer";
    private final ObjectMapper objectMapper;
    private final Map<String, User> store = new LinkedHashMap<>();

    public InMemoryUserTransformer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public User resetWithExisting(User seed) {
        store.clear();
        if (seed == null) {
            return null;
        }
        User withId = ensureId(seed);
        store.put(withId.getId(), withId);
        return withId;
    }

    private User ensureId(User user) {
        if (StringUtils.isNotBlank(user.getId())) {
            return user;
        }
        return User.builder()
                .id(UUID.randomUUID().toString())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .age(user.getAge())
                .build();
    }

    @Override
    public ResponseDefinition transform(Request request, ResponseDefinition responseDefinition, FileSource files, Parameters parameters) {
        String path = request.getUrl().split("\\?")[0];

        if ("/".equals(path)) {
            return new ResponseDefinitionBuilder()
                    .withStatus(200)
                    .withHeader("Content-Type", "text/plain")
                    .withBody("OK")
                    .build();
        }

        if (!path.startsWith("/users")) {
            return new ResponseDefinitionBuilder().withStatus(404).build();
        }

        if (path.contains("/invalid")) {
            return new ResponseDefinitionBuilder().withStatus(404).build();
        }

        RequestMethod method = request.getMethod();
        if (RequestMethod.GET.equals(method)) {
            return handleGet(request);
        }
        if (RequestMethod.POST.equals(method)) {
            return handlePost(request);
        }
        if (RequestMethod.PATCH.equals(method)) {
            return handlePatch(request);
        }
        if (RequestMethod.DELETE.equals(method)) {
            return handleDelete(request);
        }
        return new ResponseDefinitionBuilder().withStatus(405).build();
    }

    private ResponseDefinition handleGet(Request request) {
        String idFilter = getIdFilter(request);
        List<User> users;

        if (idFilter == null) {
            users = new ArrayList<>(store.values());
        } else if (idFilter.startsWith("eq.")) {
            String id = idFilter.substring(3);
            users = store.values().stream()
                    .filter(user -> id.equals(user.getId()))
                    .collect(Collectors.toList());
        } else if (idFilter.startsWith("in.(")) {
            String csv = StringUtils.removeEnd(StringUtils.removeStart(idFilter, "in.("), ")");
            List<String> ids = List.of(csv.split(","));
            users = store.values().stream()
                    .filter(user -> ids.contains(user.getId()))
                    .collect(Collectors.toList());
        } else {
            users = new ArrayList<>(store.values());
        }

        return jsonResponse(200, users);
    }

    private ResponseDefinition handlePost(Request request) {
        try {
            User newUser = objectMapper.readValue(request.getBodyAsString(), User.class);
            String validationMessage = validatePayload(newUser);
            if (validationMessage != null) {
                return errorResponse(400, validationMessage);
            }

            Optional<User> duplicate = store.values().stream()
                    .filter(user -> StringUtils.equalsIgnoreCase(user.getEmail(), newUser.getEmail()))
                    .findFirst();

            if (duplicate.isPresent()) {
                return errorResponse(409, "users_email_key");
            }

            User created = ensureId(newUser);
            store.put(created.getId(), created);
            return jsonResponse(201, List.of(created));
        } catch (Exception e) {
            return errorResponse(400, "invalid input syntax for type integer");
        }
    }

    private ResponseDefinition handlePatch(Request request) {
        try {
            String idFilter = getIdFilter(request);
            if (idFilter == null || !idFilter.startsWith("eq.")) {
                return new ResponseDefinitionBuilder().withStatus(404).build();
            }

            String id = idFilter.substring(3);
            User existing = store.get(id);
            if (existing == null) {
                return new ResponseDefinitionBuilder().withStatus(404).build();
            }

            User updatedPayload = objectMapper.readValue(request.getBodyAsString(), User.class);
            if (StringUtils.isNotBlank(updatedPayload.getFirstName())) {
                existing.setFirstName(updatedPayload.getFirstName());
            }
            if (StringUtils.isNotBlank(updatedPayload.getLastName())) {
                existing.setLastName(updatedPayload.getLastName());
            }
            if (StringUtils.isNotBlank(updatedPayload.getEmail())) {
                existing.setEmail(updatedPayload.getEmail());
            }
            if (StringUtils.isNotBlank(updatedPayload.getAge())) {
                existing.setAge(updatedPayload.getAge());
            }

            store.put(existing.getId(), existing);
            return jsonResponse(200, List.of(existing));
        } catch (Exception e) {
            return errorResponse(400, "invalid input syntax for type integer");
        }
    }

    private ResponseDefinition handleDelete(Request request) {
        String idFilter = getIdFilter(request);
        if (idFilter == null) {
            return new ResponseDefinitionBuilder().withStatus(404).build();
        }

        if (idFilter.startsWith("in.(")) {
            String csv = StringUtils.removeEnd(StringUtils.removeStart(idFilter, "in.("), ")");
            for (String id : csv.split(",")) {
                store.remove(id);
            }
            return new ResponseDefinitionBuilder().withStatus(204).build();
        }

        if (idFilter.startsWith("eq.")) {
            String id = idFilter.substring(3);
            store.remove(id);
            return jsonResponse(200, List.of());
        }

        return new ResponseDefinitionBuilder().withStatus(404).build();
    }

    private String getIdFilter(Request request) {
        try {
            List<String> values = request.queryParameter("id").values();
            if (values == null || values.isEmpty()) {
                return null;
            }
            return values.get(0);
        } catch (IllegalStateException e) {
            return null;
        }
    }

    private String validatePayload(User user) {
        if (StringUtils.isBlank(user.getEmail())) {
            return "\"email\" of relation \"users\" violates not-null constraint";
        }
        if (StringUtils.isBlank(user.getFirstName())) {
            return "\"first_name\" of relation \"users\" violates not-null constraint";
        }
        if (StringUtils.isNotBlank(user.getEmail()) && !isValidEmail(user.getEmail())) {
            return "check_email_format";
        }

        if (StringUtils.isBlank(user.getAge())) {
            return "check_age_positive";
        }

        int age;
        try {
            age = Integer.parseInt(user.getAge());
        } catch (NumberFormatException e) {
            return "invalid input syntax for type integer";
        }

        if (age <= 0 || age >= 150) {
            return "check_age_positive";
        }

        return null;
    }

    private boolean isValidEmail(String email) {
        Pattern pattern = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
        return pattern.matcher(email).matches();
    }

    private ResponseDefinition errorResponse(int status, String message) {
        Map<String, Object> errorBody = new LinkedHashMap<>();
        errorBody.put("code", null);
        errorBody.put("details", null);
        errorBody.put("hint", null);
        errorBody.put("message", message);
        return jsonResponse(status, errorBody);
    }

    private ResponseDefinition jsonResponse(int status, Object body) {
        try {
            Object prepared = prepareBody(body);
            return new ResponseDefinitionBuilder()
                    .withStatus(status)
                    .withHeader("Content-Type", "application/json")
                    .withBody(objectMapper.writeValueAsString(prepared))
                    .build();
        } catch (JsonProcessingException e) {
            return new ResponseDefinitionBuilder().withStatus(500).withBody(e.getMessage()).build();
        }
    }

    private Object prepareBody(Object body) {
        if (body instanceof List<?>) {
            List<?> items = (List<?>) body;
            List<Object> mapped = new ArrayList<>();
            for (Object item : items) {
                if (item instanceof User) {
                    mapped.add(toResponseUser((User) item));
                } else {
                    mapped.add(item);
                }
            }
            return mapped;
        }

        if (body instanceof User) {
            return toResponseUser((User) body);
        }

        return body;
    }

    private Map<String, Object> toResponseUser(User user) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", user.getId());
        response.put("first_name", user.getFirstName());
        response.put("last_name", user.getLastName());
        response.put("email", user.getEmail());
        try {
            response.put("age", Integer.valueOf(user.getAge()));
        } catch (Exception e) {
            response.put("age", 0);
        }
        return response;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public boolean applyGlobally() {
        return false;
    }
}
