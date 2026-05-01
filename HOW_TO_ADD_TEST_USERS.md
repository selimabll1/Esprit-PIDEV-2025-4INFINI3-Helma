# How to Add Test Users for Login

This guide explains how to add test users to your Helma database for testing the login functionality.

## 📋 Prerequisites

- MySQL installed and running
- Database `helma_leasing` created
- Backend application configured to connect to the database

## 🔐 Test User Credentials

After running the scripts, you'll have these test users:

| Role | Email | Password |
|------|-------|----------|
| **FOUNDER** | founder@helma.com | founder123 |
| **INVESTOR** | investor@helma.com | investor123 |
| **ADMIN** | admin@helma.com | admin123 |
| **COMPLIANCE** | compliance@helma.com | compliance123 |

## 📝 Method 1: Using MySQL Console

### Step 1: Open MySQL Console

**Windows:**
```bash
mysql -u root -p
```

**Or using MySQL Workbench:**
- Open MySQL Workbench
- Connect to your local instance
- Open a new SQL tab

### Step 2: Run the Script

**Option A: Plain Text Passwords (for testing only)**
```sql
source C:/Users/pc/IdeaProjects/helmabackend/test-users.sql
```

**Option B: BCrypt Hashed Passwords (recommended)**
```sql
source C:/Users/pc/IdeaProjects/helmabackend/test-users-bcrypt.sql
```

### Step 3: Verify Users Were Created
```sql
USE helma_leasing;
SELECT id, email, role, first_name, last_name FROM users;
```

## 📝 Method 2: Copy-Paste in MySQL Console

If the `source` command doesn't work, you can copy-paste the SQL directly:

1. Open the SQL file (`test-users.sql` or `test-users-bcrypt.sql`)
2. Copy all the content
3. Paste it into your MySQL console or MySQL Workbench
4. Execute

## 📝 Method 3: Using MySQL Workbench GUI

1. Open MySQL Workbench
2. Connect to your database
3. Click **File** → **Open SQL Script**
4. Select `test-users.sql` or `test-users-bcrypt.sql`
5. Click the **Execute** button (⚡ icon)

## 🔍 Which Script Should I Use?

### Use `test-users.sql` if:
- You're just testing and don't have password encryption set up
- Your backend stores passwords as plain text (NOT RECOMMENDED for production)
- You want quick testing without security

### Use `test-users-bcrypt.sql` if:
- Your backend uses BCrypt for password hashing (RECOMMENDED)
- You have Spring Security configured with BCryptPasswordEncoder
- You want production-like security

## 🧪 Testing the Login

### Frontend Login
1. Start your Angular frontend: `npm start`
2. Navigate to: `http://localhost:4200/auth/login`
3. Use any of the test credentials above

### Backend API Testing (Postman/cURL)
```bash
# Example login request
curl -X POST http://localhost:8082/helma/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "investor@helma.com",
    "password": "investor123"
  }'
```

## 🔧 Troubleshooting

### Error: "Table 'users' doesn't exist"
Your backend might use a different table name. Check your User entity class for the `@Table` annotation.

Common alternatives:
- `user`
- `app_user`
- `helma_user`

Modify the script to match your table name.

### Error: "Access denied"
Make sure you're using the correct MySQL credentials:
```bash
mysql -u root -p
# Enter your MySQL root password
```

### Error: "Unknown database 'helma_leasing'"
Create the database first:
```sql
CREATE DATABASE helma_leasing;
```

### Login fails with "Invalid credentials"
1. Check if your backend uses BCrypt - use `test-users-bcrypt.sql`
2. Verify the user was created: `SELECT * FROM users WHERE email = 'investor@helma.com';`
3. Check your backend authentication logic

## 🔐 Security Notes

⚠️ **IMPORTANT**: These are TEST users only!

- **Never use these credentials in production**
- **Never commit plain text passwords to Git**
- **Always use BCrypt or stronger hashing in production**
- **Change default passwords immediately**

## 📊 Verify Users in Database

Run this query to see all users:
```sql
USE helma_leasing;

SELECT 
    id,
    email,
    role,
    CONCAT(first_name, ' ', last_name) AS full_name,
    created_at
FROM users
ORDER BY role;
```

Expected output:
```
+----+------------------------+------------+------------------+---------------------+
| id | email                  | role       | full_name        | created_at          |
+----+------------------------+------------+------------------+---------------------+
|  3 | admin@helma.com        | ADMIN      | Mike Admin       | 2024-02-20 10:00:00 |
|  4 | compliance@helma.com   | COMPLIANCE | Emma Compliance  | 2024-02-20 10:00:00 |
|  1 | founder@helma.com      | FOUNDER    | John Founder     | 2024-02-20 10:00:00 |
|  2 | investor@helma.com     | INVESTOR   | Sarah Investor   | 2024-02-20 10:00:00 |
+----+------------------------+------------+------------------+---------------------+
```

## 🎯 Next Steps

After adding users:
1. ✅ Test login with each role
2. ✅ Verify role-based access (FOUNDER sees founder portal, etc.)
3. ✅ Test the leasing components you just created
4. ✅ Check that navigation works for each role

## 📞 Need Help?

If you encounter issues:
1. Check your backend logs
2. Verify database connection in `application.properties`
3. Ensure your User entity matches the table structure
4. Check if authentication endpoints are working
