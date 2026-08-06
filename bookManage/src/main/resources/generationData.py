#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
图书管理系统测试数据生成脚本
生成每个表 200 条测试数据
依赖: pip install pymysql faker
"""

import random
import sys
from datetime import datetime, timedelta
from faker import Faker
import pymysql

# 初始化 Faker（支持中文）
fake = Faker('zh_CN')

# ==================== 配置区域 ====================
DB_CONFIG = {
    'host': '81.70.93.58',
    'port': 3306,
    'user': 'Library',
    'password': 'dHMJpiB4mGeG4zte',
    'database': 'library',
    'charset': 'utf8mb4'
}

# 每个表生成的数据量
BATCH_SIZE = 200

# ==================== 辅助函数 ====================

def get_connection():
    """获取数据库连接"""
    return pymysql.connect(**DB_CONFIG)

def clear_tables(conn):
    """清空所有表（按依赖顺序倒序删除）"""
    tables = [
        'b_book_stock_log',
        'b_borrow_order',
        'b_book',
        'b_category',
        'sys_user'
    ]
    cursor = conn.cursor()
    try:
        # 先关闭外键检查
        cursor.execute("SET FOREIGN_KEY_CHECKS = 0")
        for table in tables:
            cursor.execute(f"DELETE FROM `{table}`")
            cursor.execute(f"ALTER TABLE `{table}` AUTO_INCREMENT = 1")
        cursor.execute("SET FOREIGN_KEY_CHECKS = 1")
        conn.commit()
        print("✅ 所有表已清空并重置自增ID")
    except Exception as e:
        conn.rollback()
        print(f"❌ 清空表失败: {e}")
        sys.exit(1)
    finally:
        cursor.close()

def random_date(start_date, end_date):
    """生成随机日期"""
    time_between = end_date - start_date
    days_between = time_between.days
    random_days = random.randrange(days_between)
    return start_date + timedelta(days=random_days)

def random_choice(items):
    """随机选择列表中的元素"""
    return random.choice(items) if items else None

def random_phone():
    """生成随机手机号（11位）"""
    prefixes = ['130', '131', '132', '133', '134', '135', '136', '137', '138', '139',
                '150', '151', '152', '153', '155', '156', '157', '158', '159',
                '180', '181', '182', '183', '184', '185', '186', '187', '188', '189']
    return random.choice(prefixes) + ''.join([str(random.randint(0, 9)) for _ in range(8)])

def generate_unique_order_no(existing_order_nos):
    """生成唯一的订单号"""
    while True:
        # 使用时间戳 + 6位随机数
        timestamp = datetime.now().strftime('%Y%m%d%H%M%S')
        random_suffix = str(random.randint(100000, 999999))
        order_no = f"BO{timestamp}{random_suffix}"
        if order_no not in existing_order_nos:
            existing_order_nos.add(order_no)
            return order_no

# ==================== 数据生成函数 ====================

def generate_users(conn, count):
    """生成用户数据，返回包含ID的列表"""
    cursor = conn.cursor()
    users = []
    roles = ['READER', 'LIBRARIAN', 'ADMIN']
    statuses = ['ACTIVE', 'DISABLED', 'LOCKED']

    print(f"📝 开始生成 {count} 条用户数据...")

    for i in range(count):
        username = fake.user_name() + str(random.randint(100, 999))
        # 避免重复用户名
        while len([u for u in users if u['username'] == username]) > 0:
            username = fake.user_name() + str(random.randint(100, 999))

        email = fake.email()
        # 避免重复邮箱
        while len([u for u in users if u['email'] == email]) > 0:
            email = fake.email()

        user = {
            'username': username,
            'password': '$2a$10$' + fake.md5()[:32],  # 模拟加密密码
            'email': email,
            'nickname': fake.name(),
            'phone': random_phone(),
            'avatar_url': f"https://picsum.photos/seed/{random.randint(1, 10000)}/200/200",
            'role': random.choice(roles),
            'status': random.choice(statuses),
            'created_at': random_date(datetime(2023, 1, 1), datetime(2025, 12, 31)),
        }
        users.append(user)

    # 批量插入
    sql = """
          INSERT INTO `sys_user`
          (`username`, `password`, `email`, `nickname`, `phone`, `avatar_url`, `role`, `status`, `created_at`)
          VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s) \
          """
    data = [(u['username'], u['password'], u['email'], u['nickname'],
             u['phone'], u['avatar_url'], u['role'], u['status'], u['created_at'])
            for u in users]
    cursor.executemany(sql, data)
    conn.commit()

    # 获取生成的ID并添加到用户字典中
    cursor.execute("SELECT id FROM `sys_user` ORDER BY id")
    ids = [row[0] for row in cursor.fetchall()]
    for i, user in enumerate(users):
        user['id'] = ids[i]

    cursor.close()
    print(f"✅ 成功生成 {count} 条用户数据")
    return users

def generate_categories(conn, count):
    """生成分类数据，返回包含ID的列表"""
    cursor = conn.cursor()
    categories = []

    # 预定义分类名称
    category_names = [
        '计算机科学', '人工智能', '编程语言', '算法与数据结构', '数据库',
        '计算机网络', '操作系统', '软件工程', '网络安全', '移动开发',
        '前端开发', '后端开发', '云计算', '大数据', '机器学习',
        '深度学习', '自然语言处理', '计算机视觉', '区块链', '物联网',
        '数学', '物理学', '化学', '生物学', '医学',
        '文学', '历史学', '哲学', '心理学', '经济学',
        '管理学', '法学', '教育学', '艺术学', '新闻传播学',
        '英语', '日语', '法语', '德语', '西班牙语',
        '机械工程', '电气工程', '土木工程', '化学工程', '材料科学'
    ]

    print(f"📝 开始生成 {count} 条分类数据...")

    selected_names = random.sample(category_names, min(count, len(category_names)))

    # 如果预定义名称不够，用Faker补充
    while len(selected_names) < count:
        new_name = fake.word().capitalize() + random.choice(['科学', '技术', '工程', '研究'])
        if new_name not in selected_names:
            selected_names.append(new_name)

    for i in range(count):
        category = {
            'name': selected_names[i] if i < len(selected_names) else fake.word().capitalize(),
            'description': fake.sentence(nb_words=10),
            'sort_order': i,
            'status': random.choice(['ACTIVE', 'DISABLED']),
            'created_at': random_date(datetime(2023, 1, 1), datetime(2025, 12, 31)),
        }
        categories.append(category)

    sql = """
          INSERT INTO `b_category`
              (`name`, `description`, `sort_order`, `status`, `created_at`)
          VALUES (%s, %s, %s, %s, %s) \
          """
    data = [(c['name'], c['description'], c['sort_order'], c['status'], c['created_at'])
            for c in categories]
    cursor.executemany(sql, data)
    conn.commit()

    # 获取生成的ID
    cursor.execute("SELECT id FROM `b_category` ORDER BY id")
    ids = [row[0] for row in cursor.fetchall()]
    for i, cat in enumerate(categories):
        cat['id'] = ids[i]

    cursor.close()
    print(f"✅ 成功生成 {count} 条分类数据")
    return categories

def generate_books(conn, count, categories, users):
    """生成图书数据，返回包含ID的列表"""
    cursor = conn.cursor()
    books = []
    statuses = ['ACTIVE', 'INACTIVE']

    print(f"📝 开始生成 {count} 条图书数据...")

    # 预定义一些书名和作者
    book_titles = [
        '深入理解计算机系统', '算法导论', '设计模式', '重构', '代码大全',
        '人月神话', 'Unix编程艺术', 'TCP/IP详解', '数据结构与算法分析',
        '计算机网络：自顶向下方法', '操作系统导论', '数据库系统概念',
        '人工智能：一种现代方法', '机器学习实战', 'Python编程：从入门到实践',
        'Java核心技术', 'Spring实战', '微服务架构设计', '领域驱动设计',
        '持续交付', '有效需求分析', '用户故事与敏捷方法'
    ]
    authors = [
        '张明', '李华', '王强', '刘洋', '陈静',
        '杨磊', '赵雪', '黄磊', '周涛', '吴迪',
        '徐静', '孙莉', '马丁', '刘芳', '张伟'
    ]
    publishers = [
        '机械工业出版社', '清华大学出版社', '电子工业出版社',
        '人民邮电出版社', '北京大学出版社', '科学出版社',
        '中国电力出版社', '中国水利水电出版社'
    ]

    for i in range(count):
        book = {
            'title': random.choice(book_titles) + ('' if random.random() > 0.3 else f' (第{random.randint(1,5)}版)'),
            'author': random.choice(authors),
            'isbn': f"978{random.randint(0,9)}{random.randint(0,9)}{random.randint(0,9)}" +
                    ''.join([str(random.randint(0,9)) for _ in range(10)]),
            'publisher': random.choice(publishers),
            'publish_date': random_date(datetime(2000, 1, 1), datetime(2025, 12, 31)).date(),
            'description': fake.sentence(nb_words=20),
            'cover_url': f"https://picsum.photos/seed/book_{i}/300/400",
            'category_id': random.choice(categories)['id'],
            'total_stock': random.randint(1, 100),
            'available_stock': 0,  # 后面会计算
            'status': random.choice(statuses),
            'is_deleted': 0,
            'created_at': random_date(datetime(2023, 1, 1), datetime(2025, 12, 31)),
        }
        # 可用库存 <= 总库存
        book['available_stock'] = random.randint(0, book['total_stock'])
        books.append(book)

    sql = """
          INSERT INTO `b_book`
          (`title`, `author`, `isbn`, `publisher`, `publish_date`, `description`,
           `cover_url`, `category_id`, `total_stock`, `available_stock`,
           `status`, `is_deleted`, `created_at`)
          VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s) \
          """
    data = [(b['title'], b['author'], b['isbn'], b['publisher'], b['publish_date'],
             b['description'], b['cover_url'], b['category_id'],
             b['total_stock'], b['available_stock'], b['status'],
             b['is_deleted'], b['created_at']) for b in books]
    cursor.executemany(sql, data)
    conn.commit()

    # 获取生成的ID
    cursor.execute("SELECT id FROM `b_book` ORDER BY id")
    ids = [row[0] for row in cursor.fetchall()]
    for i, book in enumerate(books):
        book['id'] = ids[i]

    cursor.close()
    print(f"✅ 成功生成 {count} 条图书数据")
    return books

def generate_borrow_orders(conn, count, users, books):
    """生成借阅订单数据"""
    cursor = conn.cursor()
    orders = []
    existing_order_nos = set()  # 用于记录已生成的订单号，避免重复

    statuses = ['PENDING', 'APPROVED', 'BORROWED', 'RETURNED', 'REJECTED']

    print(f"📝 开始生成 {count} 条借阅订单数据...")

    for i in range(count):
        user = random.choice(users)
        book = random.choice(books)

        # 预约时间
        reserved_at = random_date(datetime(2023, 1, 1), datetime(2025, 12, 31))
        status = random.choice(statuses)

        # 生成唯一订单号
        order_no = generate_unique_order_no(existing_order_nos)

        order = {
            'order_no': order_no,
            'user_id': user['id'],
            'book_id': book['id'],
            'status': status,
            'remark': fake.sentence(nb_words=5) if random.random() > 0.6 else None,
            'audit_remark': None,
            'return_condition': None,
            'reserved_at': reserved_at,
            'approved_at': None,
            'borrowed_at': None,
            'due_at': None,
            'returned_at': None,
            'created_at': reserved_at,
        }

        # 根据状态设置相应时间
        if status in ['APPROVED', 'BORROWED', 'RETURNED']:
            order['approved_at'] = reserved_at + timedelta(hours=random.randint(1, 72))
            if status in ['BORROWED', 'RETURNED']:
                order['borrowed_at'] = order['approved_at'] + timedelta(hours=random.randint(1, 24))
                order['due_at'] = order['borrowed_at'] + timedelta(days=random.randint(14, 60))
                if status == 'RETURNED':
                    order['returned_at'] = order['due_at'] + timedelta(days=random.randint(-5, 10))
                    order['return_condition'] = random.choice(['GOOD', 'DAMAGED', 'LOST'])

        if status == 'REJECTED':
            order['audit_remark'] = fake.sentence(nb_words=8)

        orders.append(order)

    sql = """
          INSERT INTO `b_borrow_order`
          (`order_no`, `user_id`, `book_id`, `status`, `remark`, `audit_remark`,
           `return_condition`, `reserved_at`, `approved_at`, `borrowed_at`,
           `due_at`, `returned_at`, `created_at`)
          VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s) \
          """
    data = [(o['order_no'], o['user_id'], o['book_id'], o['status'],
             o['remark'], o['audit_remark'], o['return_condition'],
             o['reserved_at'], o['approved_at'], o['borrowed_at'],
             o['due_at'], o['returned_at'], o['created_at']) for o in orders]
    cursor.executemany(sql, data)
    conn.commit()
    cursor.close()
    print(f"✅ 成功生成 {count} 条借阅订单数据")
    return orders

def generate_stock_logs(conn, count, books, users):
    """生成库存流水数据"""
    cursor = conn.cursor()
    logs = []

    reasons = ['采购入库', '盘点调整', '借出扣减', '归还增加', '报损处理', '调拨入库', '调拨出库']

    print(f"📝 开始生成 {count} 条库存流水数据...")

    for i in range(count):
        book = random.choice(books)
        operator = random.choice(users)
        change_amount = random.choice([-5, -3, -1, 1, 2, 5, 10, 20])
        # 让变更量更有意义
        if change_amount < 0:
            reason = random.choice(['借出扣减', '报损处理', '调拨出库'])
        else:
            reason = random.choice(['采购入库', '盘点调整', '归还增加', '调拨入库'])

        log = {
            'book_id': book['id'],
            'operator_id': operator['id'],
            'change_amount': change_amount,
            'reason': random.choice([r for r in reasons if r in reason]) if random.random() > 0.5 else reason,
            'created_at': random_date(datetime(2023, 1, 1), datetime(2025, 12, 31)),
        }
        logs.append(log)

    sql = """
          INSERT INTO `b_book_stock_log`
              (`book_id`, `operator_id`, `change_amount`, `reason`, `created_at`)
          VALUES (%s, %s, %s, %s, %s) \
          """
    data = [(l['book_id'], l['operator_id'], l['change_amount'], l['reason'], l['created_at'])
            for l in logs]
    cursor.executemany(sql, data)
    conn.commit()
    cursor.close()
    print(f"✅ 成功生成 {count} 条库存流水数据")
    return logs

# ==================== 主函数 ====================

def main():
    """主函数"""
    print("=" * 60)
    print("📚 图书管理系统测试数据生成器")
    print("=" * 60)

    try:
        conn = get_connection()
        print(f"✅ 数据库连接成功: {DB_CONFIG['database']}")

        # 清空所有表
        clear_tables(conn)

        # 按依赖顺序生成数据
        print("\n" + "=" * 60)
        users = generate_users(conn, BATCH_SIZE)

        print("\n" + "=" * 60)
        categories = generate_categories(conn, min(BATCH_SIZE, 45))

        print("\n" + "=" * 60)
        books = generate_books(conn, BATCH_SIZE, categories, users)

        print("\n" + "=" * 60)
        orders = generate_borrow_orders(conn, BATCH_SIZE, users, books)

        print("\n" + "=" * 60)
        logs = generate_stock_logs(conn, BATCH_SIZE, books, users)

        # 打印统计信息
        print("\n" + "=" * 60)
        print("📊 数据生成完成! 统计信息:")
        print("=" * 60)
        print(f"  sys_user:           {len(users)} 条")
        print(f"  b_category:         {len(categories)} 条")
        print(f"  b_book:             {len(books)} 条")
        print(f"  b_borrow_order:     {len(orders)} 条")
        print(f"  b_book_stock_log:   {len(logs)} 条")
        print("=" * 60)
        print("🎉 所有测试数据生成成功!")

        conn.close()

    except pymysql.Error as e:
        print(f"\n❌ 数据库错误: {e}")
        print("请检查:")
        print("  1. 数据库服务是否正在运行")
        print("  2. 数据库配置是否正确 (host, port, user, password, database)")
        print("  3. 数据库 'library' 是否已创建")
        sys.exit(1)
    except Exception as e:
        print(f"\n❌ 未知错误: {e}")
        import traceback
        traceback.print_exc()
        sys.exit(1)

if __name__ == '__main__':
    main()