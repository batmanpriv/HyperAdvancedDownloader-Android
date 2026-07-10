import os
import sys
from pathlib import Path

def find_kotlin_files(directory):
    """پیدا کردن تمام فایل‌های کاتلین در دایرکتوری و زیرپوشه‌ها"""
    kotlin_files = []
    for root, dirs, files in os.walk(directory):
        for file in files:
            if file.endswith(('.kt', '.kts')):
                full_path = os.path.join(root, file)
                relative_path = os.path.relpath(full_path, directory)
                kotlin_files.append({
                    'full_path': full_path,
                    'relative_path': relative_path,
                    'name': file
                })
    return kotlin_files

def create_output_file(directory, kotlin_files, output_filename='kotlin_files_output.txt'):
    """ایجاد فایل خروجی با محتوای تمام فایل‌های کاتلین"""
    
    with open(output_filename, 'w', encoding='utf-8') as output_file:
        # نوشتن هدر
        output_file.write("=" * 80 + "\n")
        output_file.write(f"خروجی اسکن فایل‌های کاتلین\n")
        output_file.write(f"دایرکتوری پایه: {directory}\n")
        output_file.write(f"تعداد کل فایل‌ها: {len(kotlin_files)}\n")
        output_file.write("=" * 80 + "\n\n")
        
        if not kotlin_files:
            output_file.write("هیچ فایل کاتلینی در این ساختار پیدا نشد!\n")
            return
        
        # پردازش هر فایل
        for index, file_info in enumerate(kotlin_files, 1):
            try:
                # خواندن محتوای فایل
                with open(file_info['full_path'], 'r', encoding='utf-8') as kotlin_file:
                    content = kotlin_file.read()
                
                # نوشتن اطلاعات فایل
                output_file.write(f"{'─' * 80}\n")
                output_file.write(f"فایل شماره {index}: {file_info['name']}\n")
                output_file.write(f"مسیر نسبی: {file_info['relative_path']}\n")
                output_file.write(f"مسیر کامل: {file_info['full_path']}\n")
                output_file.write(f"تعداد خطوط: {len(content.splitlines())}\n")
                output_file.write(f"{'─' * 80}\n")
                output_file.write("محتوای فایل:\n")
                output_file.write("-" * 40 + "\n")
                output_file.write(content)
                output_file.write("\n" + "-" * 40 + "\n\n")
                
            except UnicodeDecodeError:
                # اگر فایل با UTF-8 خوانده نشد، با encoding دیگر امتحان کن
                try:
                    with open(file_info['full_path'], 'r', encoding='cp1256') as kotlin_file:
                        content = kotlin_file.read()
                    
                    output_file.write(f"{'─' * 80}\n")
                    output_file.write(f"فایل شماره {index}: {file_info['name']} (با کدگذاری cp1256)\n")
                    output_file.write(f"مسیر: {file_info['relative_path']}\n")
                    output_file.write(f"{'─' * 80}\n")
                    output_file.write("محتوای فایل:\n")
                    output_file.write("-" * 40 + "\n")
                    output_file.write(content)
                    output_file.write("\n" + "-" * 40 + "\n\n")
                    
                except Exception as e:
                    output_file.write(f"{'─' * 80}\n")
                    output_file.write(f"فایل شماره {index}: {file_info['name']} (خطا در خواندن)\n")
                    output_file.write(f"مسیر: {file_info['relative_path']}\n")
                    output_file.write(f"خطا: {str(e)}\n")
                    output_file.write(f"{'─' * 80}\n\n")
                    
            except Exception as e:
                output_file.write(f"{'─' * 80}\n")
                output_file.write(f"فایل شماره {index}: {file_info['name']} (خطا در خواندن)\n")
                output_file.write(f"مسیر: {file_info['relative_path']}\n")
                output_file.write(f"خطا: {str(e)}\n")
                output_file.write(f"{'─' * 80}\n\n")
    
    print(f"خروجی با موفقیت در فایل '{output_filename}' ذخیره شد.")
    print(f"تعداد {len(kotlin_files)} فایل کاتلین پردازش شد.")

def main():
    # دریافت دایرکتوری جاری (جایی که اسکریپت اجرا می‌شود)
    current_directory = os.getcwd()
    
    # اگر آرگومان خط فرمان داده شد، از آن استفاده کن
    if len(sys.argv) > 1:
        current_directory = sys.argv[1]
    
    # بررسی وجود دایرکتوری
    if not os.path.exists(current_directory):
        print(f"خطا: دایرکتوری '{current_directory}' وجود ندارد!")
        sys.exit(1)
    
    print(f"شروع اسکن در دایرکتوری: {current_directory}")
    
    # پیدا کردن فایل‌های کاتلین
    kotlin_files = find_kotlin_files(current_directory)
    print(f"تعداد {len(kotlin_files)} فایل کاتلین پیدا شد.")
    
    # ایجاد فایل خروجی
    create_output_file(current_directory, kotlin_files)

if __name__ == "__main__":
    main()